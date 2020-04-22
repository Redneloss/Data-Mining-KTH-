@echo off

set argC=0
for %%x in (%*) do Set /A argC+=1

IF /I "%argC%" NEQ "1" (
	echo Data file not supplied.
	echo Usage ./plot {data-file.txt}
)

gnuplot -e filename='%1' graph.gnuplot

start graph.png
