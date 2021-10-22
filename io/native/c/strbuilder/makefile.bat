ren stddef.h stddef.h.bak
call "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
cl /c str_builder.c -O2 -nologo
lib.exe -nologo /out:strbuilder.lib /machine:x64 str_builder.obj
ren stddef.h.bak stddef.h
del str_builder.obj