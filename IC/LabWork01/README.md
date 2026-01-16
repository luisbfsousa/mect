# LabWork01 - Audio Processing and Compression

This project implements various audio processing tools including histogram analysis, quantization, audio effects, and lossy compression using DCT (Discrete Cosine Transform)

[LabWork01 Report](https://github.com/Viana03/LabWork01/blob/main/Lab01_Report.pdf)

## Building the Project

```bash
cd src
make all
cd ..
```

## Parte 1

### Ex1

```bash
cd test

../bin/wav_hist sample.wav 0 > left.txt

../bin/wav_hist sample.wav 1 > right.txt

../bin/wav_hist sample.wav mid > mid.txt

../bin/wav_hist sample.wav side > side.txt
```

```bash
../bin/wav_hist 2 sample.wav 0 > left_bin2.txt

../bin/wav_hist 8 sample.wav mid > mid_bin8.txt

../bin/wav_quant sample.wav 12 sample_12bit.wav
../bin/wav_quant sample.wav 2 sample_2bit.wav
```

```bash
gnuplot
plot "left.txt" with boxes
```

### Ex2

```bash
../bin/wav_quant sample.wav 8 sample_8bit.wav

../bin/wav_quant sample.wav 4 sample_4bit.wav

../bin/wav_quant sample.wav 12 sample_12bit.wav
```

### Ex3

```bash
../bin/wav_cmp sample.wav sample_8bit.wav
```

### Ex4

```bash
../bin/wav_effects sample.wav output_echo.wav echo 0.3 0.5

../bin/wav_effects sample.wav output_multi.wav multi_echo 0.2 5 0.6

../bin/wav_effects sample.wav output_tremolo.wav amplitude_mod 5 0.5

../bin/wav_effects sample.wav output_chorus.wav varying_delay 0.02 2
```

## Parte 2

### Ex5

```bash
../bin/text2bin text-bits.txt output.bin

../bin/bin2text output.bin recovered.txt

cmp text-bits.txt recovered.txt
```

### Ex6:

```bash
../bin/wav_quant_enc sample.wav sample_8bit.enc 8

../bin/wav_quant_dec sample_8bit.enc sample_recovered.wav

../bin/wav_cmp sample.wav sample_recovered.wav
```

## Parte 3

### Ex7

```bash
../bin/wav_stereo2mono sample.wav sample_mono.wav

../bin/dct_encoder sample_mono.wav sample.dct 1024 512 1.0

../bin/dct_decoder sample.dct sample_decoded.wav

../bin/wav_cmp sample_mono.wav sample_decoded.wav
```
