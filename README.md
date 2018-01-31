# GraphAware Natural Language Processing

[![Build Status](https://travis-ci.org/graphaware/neo4j-nlp.svg?branch=master)](https://travis-ci.org/graphaware/neo4j-nlp)
This [Neo4j](https://neo4j.com) plugin offers Graph Based Natural Language Processing capabilities.

The main module, this module, provide a common interface for underlying text processors as well as a
**Domain Specific Language** built atop stored procedures and functions making your Natural Language Processing
workflow developer friendly.

It comes in 2 versions, Community (open-sourced) and Enterprise with the following NLP features :

## Feature Matrix

| | Community Edition | Enterprise Edition |
| --- | :---: | :---: |
| Text information Extraction | ✔ | ✔ |
| ConceptNet5 Enricher | ✔ | ✔ |
| Microsoft Concept Enricher | ✔ | ✔ |
| Keyword Extraction | ✔ | ✔ |
| Topics Extraction | ✔ | ✔ |
| Similarity Computation | ✔ | ✔ |
| Apache Spark Binding for Distributed Algorithms | | ✔ |
| User Interface | | ✔ |
| ML Prediction capabilities | | ✔ |
| Entity Merging | | ✔ |
| Questions generator | | ✔ |
| Conversational Features | | ✔ |

Two NLP processor implementations are available, respectively [OpenNLP](https://github.com/graphaware/neo4j-nlp-opennlp) and
[Stanford NLP](https://github.com/graphaware/neo4j-nlp-stanfordnlp).


## Installation

From the [GraphAware plugins directory](https://products.graphaware.com), download the following `jar` files :

* `neo4j-framework` (the JAR for this is labeled "graphaware-server-enterprise-all")
* `neo4j-nlp`
* `neo4j-nlp-stanfordnlp` or `neo4j-nlp-opennlp` or both

and copy them in the `plugins` directory of Neo4j.

*Take care that the version numbers of the framework you are using match with the version of Neo4J
you are using*.  This is a common setup problem.  For example, if you are using Neo4j 3.3.0, all
of the JARs you download should contain 3.3 in their version number.

`plugins/` directory example :

```
-rw-r--r--  1 abc  staff   6108799 May 16 11:27 graphaware-nlp-3.3.1.51.2-SNAPSHOT.jar
-rw-r--r--@ 1 abc  staff  13391931 May  5 09:34 graphaware-server-enterprise-all-3.3.1.51.2.jar
-rw-r--r--  1 abc  staff  46678477 May 16 14:59 nlp-opennlp-3.3.1.51.2-SNAPSHOT.jar
```

Append the following configuration in the `neo4j.conf` file in the `config/` directory:

```
  dbms.unmanaged_extension_classes=com.graphaware.server=/graphaware
  com.graphaware.runtime.enabled=true
  com.graphaware.module.NLP.1=com.graphaware.nlp.module.NLPBootstrapper
  dbms.security.procedures.whitelist=ga.nlp.*
```

Start or restart your Neo4j database.

Note: both concrete text processors are quite greedy - you will need to dedicate sufficient memory for to Neo4j heap space.

Additionally, the following indexes and constraints are suggested to speed performance:

```
CREATE CONSTRAINT ON (n:AnnotatedText) ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Tag) ASSERT n.id IS UNIQUE;
CREATE CONSTRAINT ON (n:Sentence) ASSERT n.id IS UNIQUE;
CREATE INDEX ON :Tag(a.value);
```

### Quick Documentation in Neo4j Browser

Once the extension is loaded, you can see basic documentation on all available procedures by running
this Cypher query:

```
CALL dbms.procedures() YIELD name, signature, description
WHERE name =~ 'ga.nlp.*'
RETURN name, signature, description ORDER BY name asc;
```

## Getting Started

### Text extraction

#### Pipelines and components

The text extraction phase is done with a Natural Language Processing pipeline, each pipeline has a list of enabled components.

For example, the basic `tokenizer` pipeline has the following components :


* Sentence Segmentation
* Tokenization
* StopWords Removal
* Stemming
* Part Of Speech Tagging
* Named Entity Recognition

It is also possible to create a custom pipeline:

```
CALL ga.nlp.processor.addPipeline({textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', name: 'customStopWords', processingSteps: {tokenize: true, ner: true, dependency: false}, stopWords: '+,result, all, during', 
threadNumber: 20})
```

The available optional parameters (default values are in brackets):
* `name`: desired name of a new pipeline
* `textProcessor`: to which text processor should the new pipeline be added
* `processingSteps`: pipeline configuration (available in both Stanford and OpenNLP unless stated otherwise)
  * `tokenize` (default: true): perform tokenization
  * `ner` (default: true): Named Entity Recognition
  * `sentiment` (default: false): run sentiment analysis on sentences
  * `coref` (default: false): Coreference Resolution (identify multiple mentions of the same entity, such as "Barack Obama" and "he")
  * `relations` (default: false): run relations identification between two tokens
  * `dependency`  (default: false, StanfordNLP only): extract typed dependencies (ex.: amod - adjective modifier, conj - conjunct, ...)
  * `cleanxml`  (default: false, StanfordNLP only): remove XML tags
  * `truecase`  (default: false, StanfordNLP only): recognizes the "true" case of tokens (how they would be capitalized in well-edited text) 
* `stopWords`: specify words that are required to be ignored (if the list starts with +, the following words are appended to the default stopwords list, otherwise the default list is overwritten)
* `threadNumber` (default: 4): for multi-threading
* `excludedNER`: (default: none) specify a list of NE to not be recognized in upper case, for example for excluding `NER_Money` and `NER_O` on the Tag nodes, use ['O', 'MONEY']

To delete a pipeline, use this command:
```
CALL ga.nlp.processor.removePipeline(<pipeline-name>, <text-processor>)
```


#### Example

Let's take the following text as example :

```
Scores of people were already lying dead or injured inside a crowded Orlando nightclub,
and the police had spent hours trying to connect with the gunman and end the situation without further violence.
But when Omar Mateen threatened to set off explosives, the police decided to act, and pushed their way through a
wall to end the bloody standoff.
```

**Simulate your original corpus**

Create a node with the text, this node will represent your original corpus or knowledge graph :

```
CREATE (n:News)
SET n.text = "Scores of people were already lying dead or injured inside a crowded Orlando nightclub,
and the police had spent hours trying to connect with the gunman and end the situation without further violence.
But when Omar Mateen threatened to set off explosives, the police decided to act, and pushed their way through a
wall to end the bloody standoff.";
```

**Perform the text information extraction**

The extraction is done via the `annotate` procedure which is the entry point to text information extraction

```
MATCH (n:News)
CALL ga.nlp.annotate({text: n.text, id: id(n)})
YIELD result
MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)
RETURN result
```

Available parameters of `annotate` procedure:
  * `text`: text to annotate represented as a string
  * `id`: specify ID that will be used as `id` property of the new AnnotatedText node
  * `textProcessor` (default: "Stanford", if not available than the first entry in the list of available text processors)
  * `pipeline` (default: tokenizer)
  * `checkLanguage` (default: true): run language detection on provided text and check whether it's supported

This procedure will link your original `:News` node to an `:AnnotatedText` node which is the entry point for the graph
based NLP of this particular News. The original text is broken down into words, parts of speech, and functions.
This analysis of the text acts as a starting point for the later steps.

![annotated text](https://github.com/graphaware/neo4j-nlp/raw/master/docs/image1.png)

**Running a batch of annotations**

If you have a big set of data to annotate, we recommend to use [APOC](https://github.com/neo4j-contrib/neo4j-apoc-procedures) :

```
CALL apoc.periodic.iterate(
"MATCH (n:News) RETURN n LIMIT 500",
"CALL ga.nlp.annotate({text: n.text, id: id(n)})
YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)", {})
```

Do not run run the procedure in parallel to avoid deadlocks.

### Enrich your original knowledge

We implement external knowledge bases in order to enrich the knowledge of your current data.

As of now, two implementations are available :

* ConceptNet5
* Microsoft Concept Graph

This enricher will extend the meaning of tokens (Tag nodes) in the graph.

```
MATCH (n:Tag)
CALL ga.nlp.enrich.concept({enricher: 'conceptnet5', tag: n, depth:2, admittedRelationships:["IsA","PartOf"]})
YIELD result
RETURN result
```

The `enricher` parameter can take `microsoft` or `conceptnet5` as value, is optional and has a default value for ConceptNet5.

Please refer to the [ConceptNet Documentation](http://conceptnet.io/) for more informations about the `admittedRelationships` parameter.

Tags have now a `IS_RELATED_TO` relationships to other enriched concepts.

![annotated text](https://github.com/graphaware/neo4j-nlp/raw/master/docs/image2.png)

## List of available procedures

### Keyword Extraction

```
MATCH (a:AnnotatedText)
CALL ga.nlp.ml.textRank({annotatedText: a, stopwords: '+,other,email', useDependencies: true})
YIELD result RETURN result
```

`annotatedText` is a mandatory parameter which refers to the annotated document that is required to be analyzed.

Available optional parameters (default values are in brackets):

* `keywordLabel` (Keyword): label name of the keyword nodes
* `useDependencies` (true): use universal dependencies to enrich extracted keywords and key phrases by tags related through COMPOUND and AMOD relationships
* `dependenciesGraph` (false): use universal dependencies for creating tag co-occurrence graph (default is false, which means that a natural word flow is used for building co-occurrences)
* `cleanKeywords` (true): run cleaning procedure
* `topXTags` (1/3): set a fraction of highest-rated tags that will be used as keywords / key phrases
* `removeStopwords` (true): use a stopwords list for co-occurrence graph building and final cleaning of keywords
* `stopwords`: customize stopwords list (if the list starts with `+`, the following words are appended to the default stopwords list, otherwise the default list is overwritten)
* `respectSentences` (false): respect or not sentence boundaries for co-occurrence graph building
* `respectDirections` (false): respect or not directions in co-occurrence graph (how the words follow each other)
* `iterations` (30): number of PageRank iterations
* `damp` (0.85): PageRank damping factor
* `threshold` (0.0001): PageRank convergence threshold

For a detailed `TextRank` algorithm description, please refer to our blog post about
[Unsupervised Keyword Extraction](https://graphaware.com/neo4j/2017/10/03/efficient-unsupervised-topic-extraction-nlp-neo4j.html).

### Sentiment Detection

You can also determine whether the text presented is positive, negative, or neutral.  This procedure
requires an AnnotatedText node, which is produced by `ga.nlp.annotate` above.

```
MATCH (t:MyNode)-[]-(a:AnnotatedText) 
CALL ga.nlp.sentiment(a) YIELD result 
RETURN result;
```

This procedure will simply return "SUCCESS" when it is successful, but it will apply the `:POSITIVE`, 
`:NEUTRAL` or `:NEGATIVE` label to each Sentence.  As a result, when sentiment detection is complete,
you can query for the sentiment of sentences as such:

```
MATCH (s:Sentence)
RETURN s.text, labels(s)
```

### Language Detection

```
CALL ga.nlp.detectLanguage("What language is this in?") 
YIELD result return result
```

### NLP based filter

```
CALL ga.nlp.filter({text:'On 8 May 2013,
    one week before the Pakistani election, the third author,
    in his keynote address at the Sentiment Analysis Symposium, 
    forecast the winner of the Pakistani election. The chart
    in Figure 1 shows varying sentiment on the candidates for 
    prime minister of Pakistan in that election. The next day, 
    the BBC’s Owen Bennett Jones, reporting from Islamabad, wrote 
    an article titled Pakistan Elections: Five Reasons Why the 
    Vote is Unpredictable, in which he claimed that the election 
    was too close to call. It was not, and despite his being in Pakistan, 
    the outcome of the election was exactly as we predicted.', filter: 'Owen Bennett Jones/PERSON, BBC, Pakistan/LOCATION'}) YIELD result 
return result
```

### Cosine similarity computation

Once tags are extracted from all the news or other nodes containing some text, it is possible to compute similarities between them using content based similarity. 
During this process, each annotated text is described using the TF-IDF encoding format. TF-IDF is an established technique from the field of information retrieval and stands for Term Frequency-Inverse Document Frequency. 
Text documents can be TF-IDF encoded as vectors in a multidimensional Euclidean space. The space dimensions correspond to the tags, previously extracted from the documents. The coordinates of a given document in each dimension (i.e., for each tag) are calculated as a product of two sub-measures: term frequency and inverse document frequency.

```
CALL ga.nlp.ml.cosine.compute({}) YIELD result
```

## License

Copyright (c) 2017 GraphAware

GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program.
If not, see <http://www.gnu.org/licenses/>.
