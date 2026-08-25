# COMX-35 Floppy Interface in Emma 02

How the COMX-35 system interfaces with its floppy drive image. This is a **three-layer stack**: the CPU's I/O interface → the WD1770 FDC emulation → a raw sector image file.

## 1. The hardware being emulated: WD1770 FDC

The COMX-35's floppy controller is a **Western Digital WD1770** (the machine's `fdc.bin` ROM is the disk OS that drives it). It's emulated by the `Fdc` class in `src/fdc.cpp`. Two chips are supported — `WD1793` and `WD1770` (`#define WD1793 0` / `WD1770 1` in `fdc.h`) — and the COMX config selects `type="1770"`.

## 2. CPU ↔ FDC interface (the CDP1802 Q line trick)

The CDP1802 has a single-bit **Q output line** and only 7 I/O ports. The COMX FDC is wired to **port 2**, and the Q line multiplexes what the port access means. This is defined in `data/Xml/Comx/fdc-print-32k-80col.xml`:

```xml
<disk type="1770" drives="2">
    <iogroup>0</iogroup>
    <out type="select" q="1">2</out>   <!-- Q=1, OUT 2 → select register -->
    <out type="write"  q="0">2</out>   <!-- Q=0, OUT 2 → write register/data -->
    <in  type="read"   q="0">2</in>    <!-- Q=0, INP 2 → read register/data -->
    <in  type="intrq"  q="1">2</in>    <!-- Q=1, INP 2 → read INTRQ status -->
    <ef>4</ef>                         <!-- EF4 = DRQ (data request) line -->
    ...
</disk>
```

So the real 1802 program does:

- `SEQ` (set Q), then `OUT 2` → select which FDC register
- `REQ` (reset Q), then `OUT 2` → write to that register
- `REQ`, `INP 2` → read register
- `SEQ`, `INP 2` → read INTRQ
- `BN4` (branch on EF4) → poll DRQ for data-ready

This maps exactly onto `Fdc::out1770()` / `Fdc::in1770()` in `fdc.cpp`, which branch on `p_Computer->getFlipFlopQ()`:

```cpp
Byte Fdc::in1770() {
    if (p_Computer->getFlipFlopQ()) return intrqStatus1770();
    else                            return readRegister1770();
}
```

The `q="0"/"1"` attributes in the XML are turned into this Q-gated routing by `setOutType()/setInType()` in `iodevice.cpp` — the dispatch table is indexed `[q][iogroup][port]`. That's the whole "iogroup" mechanism: each device lives in an iogroup (the FDC is group 0), and the table resolves which device an OUT/IN hits.

## 3. The DRQ handshake (data transfer)

**DRQ (Data Request)** is the FDC's "byte-ready" strobe. It is raised by the FDC when it has a byte ready for the CPU (read) or needs a byte from the CPU (write), and it is the timing heartbeat of every sector transfer. In the COMX it is wired to **EF4**, so the 1802 polls it with `BN4` (branch on EF4).

Internally the DRQ state is the `drq_` member of `Fdc`, and it is exposed to the CPU two ways:

1. **As an EF line** — `Fdc::ef1770()` returns `drq_ ^ fdcConfiguration_.ef.reverse`. The `reverse` flag (from the XML `ef` attribute) lets the config invert polarity, because some machines wire DRQ inverted. The COMX config uses `<ef>4</ef>` with no inversion, so EF4 = DRQ directly.
2. **As a status-register bit** — `drq_` also drives bit 1 (`0x02`) of the FDC status register, so software can poll the status word instead of the EF line.

The WD1770 status register bits used by this emulator:

| Bit | Mask | Read/Write meaning |
|-----|------|--------------------|
| 0 | `0x01` | BUSY |
| 1 | `0x02` | **DRQ** (data request) |
| 2 | `0x04` | LOST DATA (read/write) / TRACK 0 (seek/step) |
| 4 | `0x10` | RECORD NOT FOUND (`endCommand(0x10)` on bad sector/track) |
| 7 | `0x80` | NOT READY (`endCommand(0x80)` on I/O error) |

### Read flow (byte-by-byte)

1. Command `0x80` (Read Sector): `readSector()` reads the **entire** 128-byte sector from the image file into `sectorBuffer_` up front (synchronous `Seek`+`Read`).
2. State machine enters `exec_ = 0x80` (initial read): loads the first byte into `data_`, sets `drq_ = 1`, `status_ |= 0x02`, and arms `fdcCycles_ = 90`.
3. The CPU sees EF4 go high and reads the data register (`REQ` + `INP 2`) → `readData()` returns `data_` and clears `drq_ = 0`, `status_ &= 0xfd`.
4. State machine enters `exec_ = 0x81` (main read). If `drq_` is **still set** here, the CPU missed the byte — `status_ |= 0x04` (LOST DATA) and a "Missed FDC data" message is logged. Otherwise it loads the next byte, re-raises `drq_ = 1`, and re-arms `fdcCycles_ = 90`.
5. After the 128th byte, `exec_ = 0x82` cleanup calls `endCommand()`, which clears BUSY and raises INTRQ.

### Write flow (byte-by-byte)

1. Command `0xa0` (Write Sector): state machine sets `exec_ = 0xa0`, `drq_ = 1`, `status_ |= 0x02` — the FDC is immediately asking for the first byte.
2. The CPU writes the data register (`REQ` + `OUT 2`) → `writeData()` stores `data_ = value` and clears `drq_ = 0`, `status_ &= 0xfd`.
3. State machine `exec_ = 0xa1` (main write): if `drq_` still set → LOST DATA. Otherwise it copies `data_` into `sectorBuffer_`, then re-raises `drq_ = 1` for the next byte.
4. After 128 bytes, `exec_ = 0xa2` calls `writeSector()` to flush the buffer to the image file.

### Timing

Each byte has a **90-cycle budget** (`fdcCycles_ = 90`). The `cycleFdc()` state machine is advanced from the CPU's cycle dispatcher, so 90 FDC cycles = a fixed amount of wall-clock time. If the 1802 doesn't service DRQ within that window, the FDC flags LOST DATA and the sector is effectively corrupted — faithfully mirroring the real chip, where a slow host drops bytes.

In short: **DRQ is a polled handshake.** FDC raises DRQ (→ EF4) → CPU reads/writes one byte (which clears DRQ) → FDC prepares the next byte and re-raises DRQ. The sector never moves as a block across the CPU bus; only the 128-byte transfer *to/from the image file* is a single block I/O.

## 4. FDC ↔ image file: raw sector dump

The FDC does **not** use the `CDiskImageFile`/`CImageFile` classes (those are used by the TU58 tape emulator `tu58.cpp` and the µPD765). The WD1770 path in `fdc.cpp` opens the image **directly as a raw `wxFFile`** and does its own seeking.

**Geometry** (from the XML):

```
sides=2, tracks=35, sectors=16, sectorlength=128
```

**The image is a raw linear sector dump — no header, no interleave.** The byte offset for track `T`, side `H`, sector `S` is computed in `readSector()`/`writeSector()`:

```cpp
offset = (track_ * numberOfSides_[drive_]) + side_;   // head-interleaved per track
offset *= numberOfSectors_;
offset += sector_;
offset *= sectorLength_;
```

So the layout is: `[track 0 head 0 (16×128)][track 0 head 1 (16×128)][track 1 head 0]...` — that's why a double-sided image is `35 × 2 × 16 × 128 = 143,360` bytes and single-sided is `71,680`. Verified against the shipped images: `dos.1.4+f&m.disk.tools.img` = 143,360 bytes, `assembler.img` = 71,680 bytes.

**Sector size is fixed at 128 bytes** (CDP1802's natural sector size). The FDC streams a sector through an internal `sectorBuffer_[1024]` byte-at-a-time with DRQ handshaking (`readData()`/`writeData()`), then commits the whole 128 bytes to/from the file in one `Seek`+`Read`/`Write`.

## 5. Geometry auto-detection from the boot sector

When the image is loaded (`setFdcDiskname()`), and again when sector 0/0/0 is written, the FDC peeks at the boot sector to sniff geometry that can override the XML defaults:

```cpp
numberOfTracks_[disk-1] = numberOfTracksPerSide_ + (sectorBuffer_[0x12] * numberOfTracksPerSide_);
numberOfSides_[disk-1]  = 1 + sectorBuffer_[0x13];
if ((sectorBuffer_[0x12]==0) && (sectorBuffer_[0x13]==0) && (sectorBuffer_[0x70]==0xff))
    numberOfSides_[disk-1] = 2;
```

Byte `0x12` = track count, `0x13` = side count, `0x70 == 0xff` is a second double-sided marker. This is the COMX DOS's own on-disk self-description.

## 6. Lifecycle / wiring summary

1. **XML** (`fdc-print-32k-80col.xml`) declares the disk block → parser fills `FdcConfiguration` (in `computerconfig.h`) with ports, Q values, EF, and geometry.
2. `Computer::configureDiskExtensions()` (`xmlemu.cpp:7682`) sees `wd1770_defined`, calls `configure1770()` + `resetFdc()`, then for each of 4 drives calls `setFdcDiskname(disk+1, dir+file)`.
3. `configure1770()` registers the Q-gated I/O routes (`FDC1770_SELECT_OUT`, `_WRITE_OUT`, `_READ_IN`, `_INTRQ_IN`, `_EF`) and the cycle hook `CYCLE_TYPE_DISK_FDC` → `cycleFdc()`.
4. At runtime: 1802 does OUT/IN on port 2 → `out1770`/`in1770` → `onCommand()` starts a command → `cycleFdc()` (called per CPU-cycle from the cycle dispatcher) advances the state machine (seek/step/read/write/format) → `readSector()`/`writeSector()` seek+read/write the raw image file.

**Notable detail:** `clearAddress` (`0xbe68` in the XML) is a RAM location the emulator zeroes when there's "no disk in drive" — the DOS ROM polls it to report a missing disk, so the emulated OS sees a proper no-disk error rather than a hang.

---

**One-sentence version:** the COMX-35 drives a WD1770 through the 1802's Q line on port 2, and the FDC emulator treats the `.img` file as a raw, headerless, head-interleaved sector dump (35 trk × 2 sides × 16 sec × 128 B) that it reads/writes with plain file seeks.

## Key files

| File | Role |
|------|------|
| `src/fdc.cpp` / `src/fdc.h` | `Fdc` class — WD1770/WD1793 emulation, command state machine, raw image I/O |
| `src/iodevice.cpp` | Q-gated `[q][iogroup][port]` I/O dispatch table |
| `src/computerconfig.h` | `FdcConfiguration` struct (ports, Q, EF, geometry) |
| `src/xmlemu.cpp:7682` | `configureDiskExtensions()` — wires config → FDC |
| `src/main.cpp:4420` | `setFdcStepRate()` — per-command step-rate timing |
| `data/Xml/Comx/fdc-print-32k-80col.xml` | Machine definition (FDC ports, geometry, disk filenames) |
| `data/Comx/Disks/*.img` | Raw sector disk images (71,680 / 143,360 bytes) |
| `src/ImageFile.cpp/.hpp` | `CImageFile`/`CDiskImageFile` — used by TU58/µPD765, **not** the COMX FDC |
