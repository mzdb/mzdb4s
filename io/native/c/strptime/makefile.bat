
call "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
cl /c strptime.cpp -O2 -nologo
lib.exe -nologo /out:strptime.lib /machine:x64 strptime.obj

del strptime.obj