### img dupe util

This is a quickly thrown together tool to help me resolve houndrets of thousands of duplicate images.
I likely made lots of mistakes. It has issues, only works on linux and is build for extreamly nieche use.

requires `findimagedupes`, `mpv`

#### Usage

run :

`findimagedupes --script - -f /tmp/fingerprint90 -P -t "90%" -R file2 file4 file5 > dupes.sh`


Compile this project.
run the jar with: `java -jar imgDupeUtil-1.jar`
edit dupes.sh to start with:

```
max=512
VIEW(){
        while [ "$(jobs -p | wc -l)" -ge "$max" ]; do :; done
        java -jar imgDupeUtil-1.jar "$@" &
}
```

after it's compleated importing the data run `java -jar imgDupeUtil-1.jar ":mpvUi"` to start the ui.
or in case you prefer no ui. run `java -jar imgDupeUtil-1.jar ":mpv"` for every entry 

clicking on the buttons in the ui will delete the entire path
