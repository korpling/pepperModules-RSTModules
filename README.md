![SaltNPepper project](./gh-site/img/SaltNPepper_logo2010.png)
# pepperModules-RSTModules
This project provides an importer to support the rs3 format (which is produces by the [RSTTool](http://www.wagsoft.com/RSTTool/)) for the linguistic converter framework Pepper (see https://u.hu-berlin.de/saltnpepper). A detailed description of the importer can be found in section [RSTImporter](#details).

Pepper is a pluggable framework to convert a variety of linguistic formats (like [TigerXML](http://www.ims.uni-stuttgart.de/forschung/ressourcen/werkzeuge/TIGERSearch/doc/html/TigerXML.html), the [EXMARaLDA format](http://www.exmaralda.org/), [PAULA](http://www.sfb632.uni-potsdam.de/paula.html) etc.) into each other. Furthermore Pepper uses Salt (see https://github.com/korpling/salt), the graph-based meta model for linguistic data, which acts as an intermediate model to reduce the number of mappings to be implemented. That means converting data from a format _A_ to format _B_ consists of two steps. First the data is mapped from format _A_ to Salt and second from Salt to format _B_. This detour reduces the number of Pepper modules from _n<sup>2</sup>-n_ (in the case of a direct mapping) to _2n_ to handle a number of n formats.

![n:n mappings via SaltNPepper](./gh-site/img/puzzle.png)

In Pepper there are three different types of modules:
* importers (to map a format _A_ to a Salt model)
* manipulators (to map a Salt model to a Salt model, e.g. to add additional annotations, to rename things to merge data etc.)
* exporters (to map a Salt model to a format _B_).

For a simple Pepper workflow you need at least one importer and one exporter.

## Requirements
Since the here provided module is a plugin for Pepper, you need an instance of the Pepper framework. If you do not already have a running Pepper instance, click on the link below and download the latest stable version (not a SNAPSHOT):

> Note:
> Pepper is a Java based program, therefore you need to have at least Java 7 (JRE or JDK) on your system. You can download Java from https://www.oracle.com/java/index.html or http://openjdk.java.net/ .


## Install module
If this Pepper module is not yet contained in your Pepper distribution, you can easily install it. Just open a command line and enter one of the following program calls:

**Windows**
```
pepperStart.bat 
```

**Linux/Unix**
```
bash pepperStart.sh 
```

Then type in command *is* and the path from where to install the module:
```
pepper> update de.hu_berlin.german.korpling.saltnpepper::pepperModules-pepperModules-RSTModules::https://korpling.german.hu-berlin.de/maven2/
```

## Usage
To use this module in your Pepper workflow, put the following lines into the workflow description file. Note the fixed order of xml elements in the workflow description file: &lt;importer/>, &lt;manipulator/>, &lt;exporter/>. The RSTImporter is an importer module, which can be addressed by one of the following alternatives.
A detailed description of the Pepper workflow can be found on the [Pepper project site](https://u.hu-berlin.de/saltnpepper). 

### a) Identify the module by name

```xml
<importer name="RSTImporter" path="PATH_TO_CORPUS"/>
```

### b) Identify the module by formats
```xml
<importer formatName="rs3" formatVersion="1.0" path="PATH_TO_CORPUS"/>
```

### c) Use properties
```xml
<importer name="RSTImporter" path="PATH_TO_CORPUS">
  <customization>
    <property key="PROPERTY_NAME">PROPERTY_VALUE</key>
  </customization>
</importer>
```

## Contribute
Since this Pepper module is under a free license, please feel free to fork it from github and improve the module. If you even think that others can benefit from your improvements, don't hesitate to make a pull request, so that your changes can be merged.
If you have found any bugs, or have some feature request, please open an issue on github. If you need any help, please write an e-mail to saltnpepper@lists.hu-berlin.de .

## Funders
This project has been funded by the [department of corpus linguistics and morphology](https://www.linguistik.hu-berlin.de/institut/professuren/korpuslinguistik/) of the Humboldt-Universität zu Berlin, the Institut national de recherche en informatique et en automatique ([INRIA](www.inria.fr/en/)) and the [Sonderforschungsbereich 632](https://www.sfb632.uni-potsdam.de/en/). 

## License
  Copyright 2009 Humboldt-Universität zu Berlin, INRIA.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.


# <a name="details">RSTImporter</a>
RST is a theory on phrase-like constructs, which mean, that a token in RST is a phrase. When mapping these data to Salt, an SToken object represents such a phrase. Since in most cases this is not the wanted behaviour, the RSTImporter provides a mechanism to tokenize the phrases into word-like structures. Therefore it takes use of the tokenizer provided by Salt which copies the way of tokenization from the TreeTagger (see: www.ims.uni-stuttgart.de/projekte/corplex/TreeTagger/ ). 
RST in general forms a tree-like structure, which is mapped as this to a Salt model. 

In RST, the primary data is represented by the textual values contained by Segment objects. The text of all Segment objects is mapped the sText value of exactly one STextualDS object. The text of a Segment object is concatenated to the sText prefixed by the blank seperator ' '. Imagine a text of segment1 'Is this example' and the text of segment2 'more complicated than it is supposed to be', than the sText value is 'Is this example more complicated than it is supposed to be'. The separator can be user defined or set to '' (empty) by taking use of the property <a href="#segmentSeparator">rstImporter.segmentSeparator</a>.

A Segment object itself is mapped to sStructure. As already mentioned, the RSTImporter provides the mechanism to tokenize a text in word-like tokens. Therefore, the text covered by a Segment object is tokenized to SToken objects. These SToken objects than are be related to SStructure object representing the Segment object via a SDominanceRelation. In case of a Segment object covers the text 'Is this example', the tokenizer will create SToken objects covering the text 'Is', 'this' and 'example'. The three tokens are dominated by one SStructure object. To avoid the tokenization, take use of the property <a href="#tokenize">rstImporter.tokenize</a>..

Each Group object is also mapped to a SStructure object. The tree-like structure given by Group objects and Relation objects related to Group or Segment objects is mapped to SStructure objects related via SDominanceRelation objects. Imagine a Group object 'grp1', containing another Group object 'grp2' and a Segment object 'seg1'. This will be mapped into a Salt model having three SStructure objects 'struct1' for 'grp1', 'struct2' for 'grp2' and 'struct3' for 'seg1'. Further, two SDominanceRelation objects 'dom1' and 'dom2' are created with 'struct1 -dom1-> struct2' and 'struct1 -dom2-> struct3'.

Since all Group and Segment objects are mapped to SStructure objects, we won't loose the information, what has been the source. Therefore, for the kind of the node 'group' or 'segment' a SAnnotation object is created and related to the SStructure object. The sName of this SAnnotation object is set to 'kind'. To change the sName, take use of the property <a href="#nodeKindName">rstImporter.nodeKindName</a>.

Also for the type attribute of a Group or Segment object an SAnnotation object is created. Its sName is set to 'type'. To adopt the sName, take use of the property <a href="#nodeTypeName">rstImporter.nodeTypeName</a>

The name of a Relation object is mapped to a SAnnotation object having the sName 'name'. To avoid, that a bunch of Relation object get the same name, an artificial number is concatenated to the name (SDominanceRelation.sName='name'+ occurance). For instance there are two Relation objects having the name 'rel', than the first will get the sName 'rel1' and the second will get the sName 'rel2'.
The type of a Relation object ( //relations/rel@type in rs3) is mapped to the sType of a created SDominanceRelation object. 

## Properties
 The table  contains an overview of all usable properties to customize the behaviour of this pepper module. The following section contains a close description to each single property and describes the resulting differences in the mapping to the salt model.
properties to customize importer behaviour

|Name of property	          |Type of property	| optional/ mandatory |default value|
|-----------------------------|-----------------|---------------------|-------------|
|rstImporter.tokenize         | yes|no          |optional             |yes          |
|simpleTokenize               | String          |optional             |--           |
|rstImporter.nodeKindName     |	String          |optional             |--           |
|rstImporter.nodeTypeName     |	String          |optional             |--           |
|rstImporter.relationTypeName |	String          |optional             |--           |
|rstImporter.segmentSeparator |	String          |optional             |' ' (Blank)  |

### <a name="tokenize">rstImporter.tokenize</a>
This parameter is an optional parameter and can be set to “yes” or “no”. If it is set to “yes”, the text being included in a segment will be tokenized. The tokens will be mapped to SToken-objects in Salt and attached to the SDocumentGraph-object. Further, an STextualRelation between a token and the text will be created and a dominance relation between the token and the segment. The default configuration of this parameter is true, if non tokenization is required, this parameter must explicitly set to false.

### <a name="simpleTokenize">simpleTokenize</a>
Switches on a very simple tokenization. With this property you can pass a list of characters, which should be used as separators to find the borders of tokens e.g. ' ', '.' to use a blank and a dot. Note that using this property will overwrite the default TreeTagger tokenizer. This property needs rstImporter.tokenize to be set to true.
```xml
<property key="simpleTokenize">' ','.'</key>
```

### <a name="nodeKindName">rstImporter.nodeKindName</a>
Name of the property to specify the sName of the SAnnotattion to which the kind of a node (segment or group) is mapped.

### <a name="nodeTypeName">rstImporter.nodeTypeName</a>
Name of the property to specify the sName of the SAnnotation to which the type attribute of a node is mapped.

### <a name="relationTypeName">rstImporter.relationTypeName</a>
Name of the property to specify the sName of the SAnnotation to which the name attribute of a relation is mapped to.

### <a name="segmentSeparator">rstImporter.segmentSeparator</a>
A property to add a a separator like a blank between the text of segments, when it is concatenated to the primary text in STextualDS.For instance the segment text 'Is' of segment1 and the segment text 'this' of segment2 will be concatenated to an sText value 'is'SEPARATOR'this'.
