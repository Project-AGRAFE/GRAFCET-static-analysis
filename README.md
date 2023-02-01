# GRAFCET-static-analysis
Static analysis algorithms for IEC 60848 GRAFCET.

Provided are the following analyses:
* [Abstract interpretation](https://github.com/Project-AGRAFE/GRAFCET-static-analysis/blob/main/de.hsu.grafcet.staticAnalysis/src/de/hsu/grafcet/staticAnalysis/analysis/AbstractInterpreter.java)
* Structural analysis regarding the [reachability and concurrency of steps](https://github.com/Project-AGRAFE/GRAFCET-static-analysis/blob/main/de.hsu.grafcet.staticAnalysis/src/de/hsu/grafcet/staticAnalysis/analysis/StructuralConcurrencyAnalyzer.java)

## Dependencies
The provided plugin has to be integrated into the [GRAFCET-editor](https://github.com/Project-AGRAFE/GRAFCET-editor) as an Eclipse-plugin since it is based on the [GRAFCET meta-model](https://github.com/Project-AGRAFE/GRAFCET-meta-model).
The abstract interpertation algorithm is based on the [C-based library Apron](https://github.com/antoinemine/apron). Apron has to be installed. An [Eclipse-plugin containing .jar files of a Java-wrapper](https://github.com/Project-AGRAFE/GRAFCET-static-analysis/tree/main/de.hsu.grafcet.lib.apron) is provided. To use Apron Linux is recomended.

## Getting started
Once the editor is set up, this plugin is integrated, the editor is started and a [GRAFCET-instance](https://github.com/Project-AGRAFE/GRAFCET-instances) is created the algorithms can be executed by using the context menue on an .grafcet-file in the Eclipse runtime. Log files containing the results will be created.

## How to cite
_tbd_

## References
_tbd_
