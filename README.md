# GenPat

* [I. Introduction of GenPat](#user-content-i-introduction)
* [II. Environment setup](#user-content-ii-environment)
* [III. Run GenPat Step-by-Step](#user-content-iii-how-to-run)
* [IV. Evaluation Result](#user-content-iv-evaluation-result)
* [V. Structure of the project](#user-content-v-structure-of-the-project)

## I. Introduction

*GenPat* is an automatic program transformation inferring framework, which infers general (reusable) transformations from singular examples. In this process, it leverages a big code corpus to guide the inference process. The following figure is the workflow of our approach.

![The workflow of this technique.\label{workflow}](./doc/figure/overview.png)

#### Inferring Stage

1. **Extracting Hypergraphs** : parses source code before and after changing into hypergraph representations with a set of attributes and relations in the graph.
2. **Extracting Modifications** : performs hypergraph differencing and extracts modifications related to corresponding nodes in the graph. Specifically, tree-based differencing, e.g., GumTree, can be used to perfrom the differencing as this hypergraph is a super graph with the original AST as backbone.
2. **Inferring Transformations** : based on the previous process, we know which nodes are modified. This step first selects a set of node according to the expansion starting from the changed nodes, which will constitute the context of the transformation. Next, a big code corpus will be utilized to select the attributes of those context nodes, i.e., frequently appeared attributes across projects will be kept in the transformation pattern, while others discarded.

As a result, the final transformation pattern contains a subset of the nodes in the hypergraph and their selected attributes.

#### Applying Stage

1. **Matching Hypergraph** : when given a transformation pattern and an examplar code snippet, *GenPat* first extracts a hypergraph from the code and tries to match it with the given pattern, where all nodes and attributes in the pattern should be matched with some one in the hypergraph of the examplar code.
2. **Migrating Modifications** : after matching the hypergraph with the pattern, a sequnence of mappings can be obtained to record the matching result of the hypergraphs. According to the mappings, modifications related to corresponding nodes can be transferred to the target hypergraph with replacing mapped variables and expressions.

*If you want to use this project, please cite our technical paper published on ASE'19.*

```tex
@inproceedings{GenPat:2019,
    author   = {Jiang, Jiajun and Ren, Luyao and Xiong, Yingfei and Zhang, Lingming},
    title    = {Inferring Program Transformations From Singular Examples via Big Code},
    series   = {ASE},
    year     = {2019},
    location = {San Diego, California, U.S.},
} 
```

## II. Environment

* OS: Linux (Tested on Ubuntu 16.04.2 LTS)
* JDK: Oracle jdk1.8 (**important!**)
* Mavan: Apache Maven 3.6
* Download and configure Defects4J.
* More runtime configurations can be found in the [config-file](/resources/conf).


## III. How to run

*GenPat* was traditionally developed as a Maven project via IntelliJ, you can simply import this project to your workspace and run it as a common Maven program or contribute it. The main class is **mfix.Main**, and for the running option please refer to the [Running Options](#user-content-running-options).

* As it is a maven project, a simpler method to build it is using the command line with `mvn package`, which will generate a runnable `jar` file in the `artifact` directory. (NOTE: Please ensure you have configured __Java & Maven__ environment first)

#### Running Options 

Our prototype of *GenPat* has multiple user interfaces for different purposes, basically you can run it within command line as:

```powershell
java -jar GenPat.jar FUNC ARGS
```

where the `FUNC` denotes the function/interface you want to use and `ARGS` is a set of arguments corresponding to the `FUNC`.

The following details some of the `FUNC` that are important:

```powershell
print: print the details of pattern
filter: filter and serialize patterns with given criteria.
repair: run porgram repair
cluster: run pattern cluster
```

More Details related to each `FUNC`: 

* **print**: `"<command> -if <arg> [-of <arg>]"`, printing the details of the given pattern to console or file.

  * `-if`: denotes the absolute path of pattern file (e.g., [xxx/example/patterns/pattern_file1.pattern](./example/patterns), serialized file).

  * `-of`: denotes the absolute path of the output file.

    e.g., java -jar GenPat.jar print -if in_file -of out_file

* **filter**: `"<command> (-ip <arg> | -filter <arg>) [-dir <arg>] [-line <arg>] [-change <arg>] [-of <arg> | -op <arg>]"`, filter and serialize patterns with the given criteria.

  * Option group, one of the following options should be given

    1. `-ip`: denotes the #Directory# of the pairwise buggy-fixed files (e.g., [xxx/example/src](./example/src)). #Directory# should be formatted as follows:

      ```powershell
      |-directory
        |...|-buggy-version
        |...|-| source_code_file.java
        |...|-fixed-version
        |...|-| source_code_file.java
      ```
    
      One can reformat the structure of files in [`mfix.common.util.MiningUtils.java`](./src/main/java/mfix/common/util/MiningUtils.java)

    2. `-filter`: denotes the absolute path of the file that contains the paths of patterns to be filtered (e.g., [xxx/example/records/PatternRecord.txt](./example/records/)).

  * `-dir`: the output directory of the serialized patterns (needed only when option `-filter` is used, e.g., if the option for `-filter` is file [PatternRecord.txt](./example/records/), `-dir` can be `/home/jack/code`, `/home/jack` and so on, which is the prefix of the path.

  * `-line`: max number of lines of code that were changed (filtering criterion).

  * `-change`: max number of change actions (filtering criterion).

  * `-of | -op`: the `-of` denotes the absolute path of output file for recording the paths of patterns, while `-op` denotes the absolute path of output directory (default file name is "PatternRecord.txt").

* **repair**: `"<command> (-bf <arg> | -bp <arg> | -xml | -d4j <arg>) (-pf <arg> | -pattern <arg>) [-d4jhome <arg>]"`, try to repair a buggy file or a bug in defects4j with the given pattern or list of patterns. One argument in each bracket should be given.

  * Option group, one of the following options should be given:
    1. `-bf`: denotes the absolute path of buggy file.
    2. `-bp`: denotes the absolute base directory of buggy program or file (GenPat will traverse the files in the directory for repair).
    3. `-xml`: **no arguments** for this option, denotes reading the buggy subject information from [project.xml](./resources/conf/).
    4. `-d4j`: denotes the bug id in defects4j benchmark, e.g., chart_1.
  * Option group, one of the following options should be given:
    1. `-pf`: denotes the absolote path of the file which records the paths of all avaliable patterns (e.g., [xxx/example/records/PatternRecord.txt](./example/records/)).
    2. `-pattern`: denotes the absolote path of pattern file (e.g., [xxx/example/patterns/pattern_file1.pattern](./example/patterns)).
  * `-d4jhome`: denotes the home directory of defects4j, used for running defects4j command (e.g., `/home/jack/code/defects4j`).

* **cluster**: `"<command> -if <arg> [-dir <arg>] [-op <arg>]"`, cluster same patterns together.

  * `-if`: denotes the absolute path of the file which contains the path information of patterns (e.g., [xxx/example/records/PatternRecord.txt](./example/records/)).
  * `-dir`: the home directory of pattern files, it can be `/home/jack/code`, `/home/jack` and so on, which is the prefix of the path in [PatternRecord.txt](./example/records/)).
  * `-op`: the output path of results.

We only list some of the most important features of *GenPat*, please learn more via checking its implementation.

#### Result Analysis

* Typically, the output is the transformed source code after applying some pattern.
* Particularly, when using *GenPat* for program repair on defects4j, it will produce two sub-folders, i.e., log and patch, where the log folder contains the log information during the repair, including used pattern, code-diffs and so on (view [log](./repair-result/log) for more details), while the patch folder contains the patches that can pass the test cases. (view [patch](./repair-result/patch) for more details).

## IV. Evaluation Result

Evaluations on two distinct application scenarios:

* Systematic Editing—**significantly outperforms the state-of-the-art Sydit with up to 5.5x correctly transformed cases.**

* Automatic Program Repair —**successfully fixed 19 bugs in the Defects4J benchmark, 4 of which have never been repaired by any existing technique.**

  Please read our paper for more evaluation results.

## V. Structure of the project
```powershell
  |--- README.md      :  user guidance
  |--- resources      :  resource files and configurations
  |--- doc            :  document
  |--- final          :  evaluation result
  |--- lib            :  dependent libraries
  |--- repair-result: :  evaluation result in our paper for program repair
  |--- replication    :  replication package with sampled cases
  |--- sbfl           :  fault localization tool
  |--- src            :  source code and test suite
```

----


<u>__ALL__ suggestions and contributions are welcomed.</u>
