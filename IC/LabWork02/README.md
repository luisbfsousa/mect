# LabWork02

[LabWork02 Report](https://github.com/Viana03/LabWork02/blob/main/Lab02_Report.pdf)

## Compiling
```bash
cd src
make all
cd ..
cd bin
```

## Exercise 1

```bash
./extract_channel ../images/arial.ppm ../output/arialC1.ppm 1
```  
To check the results view the image at the output directory

<br>

## Exercise 2

**Negative image**
```bash
./negative_img ../images/monarch.ppm ../output/monarch_negative.ppm
```

**Mirror image in horizontal axis**
```bash
./mirror_img ../images/bike3.ppm ../output/bike_mirrorh.ppm h
```

**Mirror image in vertical axis**
```bash
./mirror_img ../images/bike3.ppm ../output/bike_mirrorv.ppm v
```

**Rotated image (multiples of 90º)**
```bash
./rotate_img ../images/girl.ppm ../output/girl_rotated.ppm 90
```

**Modifying brightness**
```bash
./modifyLight_img ../images/airplane.ppm ../output/airplanemodified.ppm 175
```
To check the results view the image at the output directory

<br>

## Exercise 3
We made a file with the purpose to test this exercise
```bash
./test_golomb 
```  

<br>

## Exercise 4
**Encode audio parameters**
```bash
./audio_encode ../audio/sample01.wav ../output/sample01enc.golb
```  
or
```bash
 ./audio_encode ../audio/sample01.wav ../output/sample01.golb -b 1024 -i
 ``` 
-b 1024 sets block size (default 1024)

-i enables mid-side transform for stereo (inter-channel prediction)

**Decode audio parameters**
```bash
./audio_decode ../output/sample01enc.golb ../output/sample01dec.wav
``` 


## Exercise 5
**Encode image parameters**
```bash
./image_encode ../images/boat.ppm ../output/boat_enc.golb
```

**Decode image parameters**
```bash
./image_decode ../output/boat_enc.golb ../output/boat_dec.ppm
```

To check the results view the image at the output directory, with the same size as the original