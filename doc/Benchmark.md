Benchmarking CPAchecker
=======================

For benchmarking, it is recommended to use
[BenchExec](https://github.com/dbeyer/benchexec).
An extended version of BenchExec is bundled with CPAchecker
and usable by calling `scripts/benchmark.py`.
This script should run with any Python version >= 3.2.
This script takes as input one or more XML files which specify the tool,
the resource limits, the tool configuration, the input programs
and the columns that should appear in the output table.
The script puts all the results into the test/results/ directory.
Commented examples for these XML files are given in the BenchExec 
[documentation](https://github.com/dbeyer/benchexec/tree/master/doc)
and in `doc/examples/benchmark*.xml`.
The file [doc/examples/benchmark-cpachecker.xml](doc/examples/benchmark-cpachecker.xml)
can be used as base for own benchmarks.
Several useful benchmark configurations are in `test/test-sets/*.xml`.

For further help on the benchmark script use `scripts/benchmark.py -h`.

Specifications
--------------
If the benchmark script should evaluate whether the returned results
are correct or wrong and calculate scores like in the SV-COMP,
there needs to be a specification defined in the benchmark definition
that tells the script what should be checked (error label reachability,
memory safety, termination, etc.).
The specification is given in a property file (`.prp`) just like in the SV-COMP.
There are examples for such files in `test/programs/benchmarks/property*.prp`.
To specify them in the benchmark, use the `<propertyfiles>` tag.
It can occur either in global scope or inside a `<sourcefiles>` tag.
You can use the variables that will be resolved by the benchmark scripts
like in other tags, for example:
`<propertyfile>${sourcefile_path}/ALL.prp</propertyfile>`
This will verify each source file against the specification ALL.prp
in the same directory.
You do not need to specify a separate command-line option
with the specification for the verifier,
as the script will automatically add it to the command line.

Specifying Machine Model and Further Program Characteristics
------------------------------------------------------------

It is important to specify the correct values for the following options
according to the characteristics of the input files to avoid wrong answers:

- Machine model (32 vs. 64 bit):
  32-bit model is assumed by default.
  For 64 bit, specify `-64` on the command line.

- Whether the "simple" memory model of SV-Comp is used or not
  (cf. http://sv-comp.sosy-lab.org/2016/rules.php#programs):
  If yes, set `cpa.predicate.handlePointerAliasing=false`.

- Whether `malloc` may return null or not:
  If the program assumes `malloc` never returns null,
  set `cpa.predicate.memoryAllocationsAlwaysSucceed=true`.
  Note that this assumption is true for all SV-Comp files,
  thus this option is already set in all `-sv-comp*` configurations.


Specifying Resource Limits
--------------------------
When benchmarking CPAchecker, it is important to correctly specify
the time and memory limits.

- *CPU Time*.
  Firstly, in order to get statistics even in case of a timeout,
  is is important to specify different "soft" and "hard" CPU-time limits
  like in this example:
  `<benchmark timelimit="900" hardtimelimit="1000" ...`
  The soft time limit is automatically passed as parameter to CPAchecker,
  so there is no need to specify the `-timelimit` option manually.

- *Memory*.
  The memory limit is specified in Megabytes
  (SI units, i.e., 1 MB = 1,000,000 Bytes)
  with the attribute `memlimit` in the `<benchmark>` tag
  of the benchmark definition XML file. Example:
  `<benchmark ... memlimit="8000">`
  This limit will be enforced by the OS
  and CPAchecker will be killed if it needs more memory.

Additionally, it is important to specify the amount of memory
that Java uses for its own heap with the `-heap` command-line parameter.
This value needs to be lower than the external limit.
Setting it too low will hurt the performance due to increased garbage collection
and provoke `OutOfMemoryError`,
setting it too high limits the memory that is available to native libraries
such as MathSAT.
For analyses without MathSAT,
start to experiment with 1000 MB less than the external limit.
IMPORTANT: Java does not use SI units here, but IEC units (factor 1024).
7000M here are 5% more than 7000 for the memory limit above!
Example:
`<option name="-heap">7000M</option>`

Summary:
For correct and useful benchmarks, choose a memory limit (e.g., 8000MB),
a Java heap size (e.g., 7000MiB), and a timelimit (e.g., 900s).
Then specify them as follows:

```xml
<benchmark ... timelimit="900" hardtimelimit="1000" memlimit="8000">
  <option name="-heap">7000M</option>
  ...
</benchmark>
```


Useful CPAchecker Options
-------------------------
There are several CPAchecker options that are especially useful for benchmarks:

- `output.disable=true`
  This option disables all output files that would otherwise be
  generated by CPAchecker.
  This is also available as command-line argument `-noout`.

- `statistics.memory=false`
  This option disables the additional thread that is tracking
  the memory usage over time.
  Use this if the last few ns of CPU time
  are more important than memory statistics.

It is also recommended to disable assertion checking for benchmarks.
This can be done by adding the command-line argument `-disable-java-assertions`.


Result Table Generation
-----------------------
In order to combine the results from several benchmark runs into
a single table the script scripts/table-generator.py can be used.
It also takes an XML file as input that gives the table configuration
and is described in the
[table-generator documentation](https://github.com/dbeyer/benchexec/blob/master/doc/table-generator.md).

The output will be tables in HTML and CSV format,
the former having some additional features like showing the log files
and being able to plot the results.