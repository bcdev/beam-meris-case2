@echo off

// Adopt this path to your needs
set BEAM4_HOME=C:\Programme\beam\beam-4.1

"%BEAM4_HOME%\jre\bin\java.exe" ^
    -Xmx1024M ^
    -Dceres.context=beam ^
    "-Dbeam.mainClass=org.esa.beam.framework.processor.ProcessorRunner" ^
    "-Dbeam.processorClass=org.esa.beam.lakes.boreal.processor.BorealLakesProcessor" ^
    "-Dbeam.home=%BEAM4_HOME%" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%BEAM4_HOME%\modules\lib-hdf-2.3\lib\win\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%BEAM4_HOME%\modules\lib-hdf-2.3\lib\win\jhdf5.dll" ^
    -jar "%BEAM4_HOME%\bin\ceres-launcher.jar" %*

exit /B 0
