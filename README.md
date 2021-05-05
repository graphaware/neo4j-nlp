# GraphAware Natural Language Processing

## GraphAware Natural Language Processing Has Been Retired
As of May 2021, this [repository has been retired](https://graphaware.com/framework/2021/05/06/from-graphaware-framework-to-graphaware-hume.html).

---

This [Neo4j](https://neo4j.com) plugin offers Graph Based Natural Language Processing capabilities.

The main module, this module, provide a common interface for underlying text processors as well as a
**Domain Specific Language** built atop stored procedures and functions making your Natural Language Processing
workflow developer friendly.

It comes in 2 versions, Community (open-sourced) and Enterprise with the following NLP features :

## Feature Matrix

| | Community Edition | Enterprise Edition |
| --- | :---: | :---: |
| Text information Extraction | ✔ | ✔ |
| Multi-languages in the same database | | ✔ |
| Custom NamedEntityRecognition model builder | | ✔ |
| ConceptNet5 Enricher | ✔ | ✔ |
| Microsoft Concept Enricher | ✔ | ✔ |
| Keyword Extraction | ✔ | ✔ |
| TextRank Summarization | ✔ | ✔ |
| Topics Extraction | | ✔ |
| Word Embeddings (Word2Vec) | ✔ | ✔ |
| Similarity Computation | ✔ | ✔ |
| PDF Parsing | ✔ | ✔ |
| Apache Spark Binding for Distributed Algorithms | | ✔ |
| Doc2Vec implementation | | ✔ |
| User Interface | | ✔ |
| ML Prediction capabilities | | ✔ |
| Entity Merging | | ✔ |

Two NLP processor implementations are available, respectively [Stanford NLP](https://github.com/graphaware/neo4j-nlp-stanfordnlp) and
[OpenNLP](https://github.com/graphaware/neo4j-nlp-opennlp) (OpenNLP receives less frequent updates, StanfordNLP is recommended).


## Installation

*From version 3.5.1.53.15 you need to download the language models, see below*

From the [GraphAware plugins directory](https://products.graphaware.com), download the following `jar` files :

* `neo4j-framework` (the JAR for this is labeled "graphaware-server-enterprise-all")
* `neo4j-nlp`
* `neo4j-nlp-stanfordnlp`
* The language model to be downloaded from `https://stanfordnlp.github.io/CoreNLP/#download`

and copy them in the `plugins` directory of Neo4j.

*Take care that the version numbers of the framework you are using match with the version of Neo4J
you are using*.  This is a common setup problem.  For example, if you are using Neo4j 3.4.0 and above, all
of the JARs you download should contain 3.4 in their version number.

`plugins/` directory example :

```
-rw-r--r--  1 ikwattro  staff    58M Oct 11 11:15 graphaware-nlp-3.5.1.53.14.jar
-rw-r--r--@ 1 ikwattro  staff    13M Aug 22 15:22 graphaware-server-community-all-3.5.1.53.jar
-rw-r--r--  1 ikwattro  staff    16M Oct 11 11:28 nlp-stanfordnlp-3.5.1.53.14.jar
-rw-r--r--@ 1 ikwattro  staff   991M Oct 11 11:45 stanford-english-corenlp-2018-10-05-models.jar
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
CREATE INDEX ON :Tag(value);
```

Or use the dedicated procedure :

```
CALL ga.nlp.createSchema()
```

Define which language you will use in this database :

```
CALL ga.nlp.config.setDefaultLanguage('en')
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

It is mandatory to create your pipeline first :

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
  * `customNER`: list of custom NER model identifiers (as a string, model identifiers separated by “,”)
* `stopWords`: specify words that are required to be ignored (if the list starts with +, the following words are appended to the default stopwords list, otherwise the default list is overwritten)
* `threadNumber` (default: 4): for multi-threading
* `excludedNER`: (default: none) specify a list of NE to not be recognized in upper case, for example for excluding `NER_Money` and `NER_O` on the Tag nodes, use ['O', 'MONEY']


To set a pipeline as a default pipeline:

```
CALL ga.nlp.processor.pipeline.default(<your-pipeline-name>)
```

To delete a pipeline, use this command:

```
CALL ga.nlp.processor.removePipeline(<pipeline-name>, <text-processor>)
```

To see details of all existing pipelines:

```
CALL ga.nlp.processor.getPipelines()
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
"MATCH (n:News) RETURN n",
"CALL ga.nlp.annotate({text: n.text, id: id(n)})
YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)", {batchSize:1, iterateList:true})
```

It is **important** to keep the `batchSize` and `iterateList` options as mentioned in the example. Running the annotation
procedure in parallel will create deadlocks.

### Enrich your original knowledge

We implement external knowledge bases in order to enrich the knowledge of your current data.

As of now, two implementations are available :

* ConceptNet5
* Microsoft Concept Graph

This enricher will extend the meaning of tokens (Tag nodes) in the graph.

```
MATCH (n:Tag)
CALL ga.nlp.enrich.concept({enricher: 'conceptnet5', tag: n, depth:1, admittedRelationships:["IsA","PartOf"]})
YIELD result
RETURN result
```

The available parameters (default values are in brackets):
* `tag`: tag to be enriched
* `enricher` (`"conceptnet5"`): choose `microsoft` or `conceptnet5`
* `depth` (`2`): how deep to go in concept hierarchy
* `admittedRelationships`: choose desired concept relationships types, please refer to the [ConceptNet Documentation](http://conceptnet.io/) for details
* `pipeline`: choose pipeline name to be used for cleansing of concepts before storing them to your DB; your system default pipeline is used otherwise
* `filterByLanguage` (`true`): allow only concepts of languages specified in `outputLanguages`; if no languages are specified, the same language as `tag` is required
* `outputLanguages` (`[]`): return only concepts with specified languages
* `relDirection` (`"out"`): desired direction of relationships in concept hierarchy (`"in"`, `"out"`, `"both"`)
* `minWeight` (`0.0`): minimal admitted concept relationship weight
* `limit` (`10`): maximal number of concepts per `tag`
* `splitTag` (`false`): if `true`, `tag` is first tokenised and then individual tokens enriched

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
* `respectSentences` (false): respect or not sentence boundaries for co-occurrence graph building
* `respectDirections` (false): respect or not directions in co-occurrence graph (how the words follow each other)
* `iterations` (30): number of PageRank iterations
* `damp` (0.85): PageRank damping factor
* `threshold` (0.0001): PageRank convergence threshold
* `removeStopwords` (true): use a stopwords list for co-occurrence graph building and final cleaning of keywords
* `stopwords`: customize stopwords list (if the list starts with `+`, the following words are appended to the default stopwords list, otherwise the default list is overwritten)
* `admittedPOSs`: specify which POS labels are considered as keyword candidates; needed when using different language than English
* `forbiddenPOSs`: specify list of POS labels to be ignored when constructing co-occurrence graph; needed when using different language than English
* `forbiddenNEs`: specify list of NEs to be ignored

For a detailed `TextRank` algorithm description, please refer to our blog post about
[Unsupervised Keyword Extraction](https://graphaware.com/neo4j/2017/10/03/efficient-unsupervised-topic-extraction-nlp-neo4j.html).

Using universal dependencies for keyword enrichment (`useDependencies` option) can result in keywords with unnecessary level of detail, for example a keyword *space shuttle logistics program*. In many use cases we might be interested to also know that given document speaks generally about *space shuttle* (or *logistic program*). To do that, run post-processing with one of these options:
* `direct` - each key phrase of *n* number of tags is checked against all key phrases from all documents with *1 < m < n* number of tags; if the former contains the latter key phrase, then a `DESCRIBES` relationship is created from the *m*-keyphrase to all annotated texts of the *n*-keyphrase
* `subgroups` - the same procedure as for `direct`, but instead of connecting higher level keywords directly to *AnnotatedTexts*, they are connected to the lower level keywords with `HAS_SUBGROUP` relationships
```
// Important note: create subsequent indices to optimise the post-process method performance
CREATE INDEX ON :Keyword(numTerms)
CREATE INDEX ON :Keyword(value)

CALL ga.nlp.ml.textRank.postprocess({keywordLabel: "Keyword", method: "subgroups"})
YIELD result
RETURN result
```
`keywordLabel` is an optional argument set by default to *"Keyword"*.

The postprocess operation by default is processing on all keywords, which can be very heavy on large graphs. You can specify the annotatedText on which to apply the postprocess operation with the `annotatedText` argument :

```
MATCH (n:AnnotatedText) WITH n LIMIT 100
CALL ga.nlp.ml.textRank.postprocess({annotatedText: n, method:'subgroups'}) YIELD result RETURN count(n)
```

Example for running it efficiently on the full set of Keywords with APOC :

```
CALL apoc.periodic.iterate(
'MATCH (n:AnnotatedText) RETURN n',
'CALL ga.nlp.ml.textRank.postprocess({annotatedText: n, method:"subgroups"}) YIELD result RETURN count(n)',
{batchSize: 1, iterateList:false}
)
```

### TextRank Summarization

Similar approach to the keyword extraction can be employed to implement simple summarization. A densely connect graph of sentences is created, with Sentence-Sentence relationships representing their similarity based on shared words (number of shared words vs sum of logarithms of number of words in a sentence). PageRank is then used as a centrality measure to rank the relative importance of sentences in the document.

To run this algorithm:
```
MATCH (a:AnnotatedText)
CALL ga.nlp.ml.textRank.summarize({annotatedText: a}) YIELD result
RETURN result
```
Available parameters:
* `annotatedText`
* `iterations` (30): number of PageRank iterations
* `damp` (0.85): PageRank damping factor
* `threshold` (0.0001): PageRank convergence threshold

The summarisation procedure saves new properties to Sentence nodes: `summaryRelevance` (PageRank value of given sentence) and `summaryRank` (ranking; 1 = highest ranked sentence). Example query for retrieving summary:
```
match (n:Kapitel)-[:HAS_ANNOTATED_TEXT]->(a:AnnotatedText)
where id(n) = 233
match (a)-[:CONTAINS_SENTENCE]->(s:Sentence)
with a, count(*) as nSentences
match (a)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:HAS_TAG]->(t:Tag)
with a, s, count(distinct t) as nTags, (CASE WHEN nSentences*0.1 > 10 THEN 10 ELSE toInteger(nSentences*0.1) END) as nLimit
where nTags > 4
with a, s, nLimit
order by s.summaryRank
with a, collect({text: s.text, pos: s.sentenceNumber})[..nLimit] as summary
unwind summary as sent
return sent.text
order by sent.pos
```

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
MATCH (a:AnnotatedText) 
//WHERE ...
WITH collect(a) as nodes
CALL ga.nlp.ml.similarity.cosine({input: <list_of_annotated_texts>[, query: <tfidf_query>, relationshipType: "CUSTOM_SIMILARITY", ...]}) YIELD result
RETURN result
```

Available parameters (default values are in brackets):
* `input`: list of input nodes - AnnotatedTexts
* `relationshipType` (SIMILARITY_COSINE): type of similarity relationship, use it along with `query`
* `query`: specify your own query for extracting *tf* and *idf* in form `... RETURN id(Tag), tf, idf`
* `propertyName` (value): name of an existing node property (array of numerical values) which contains already prepared document vector


### Word2vec

Word2vec is a shallow two-layer neural network model used to produce word embeddings (words represented as multidimensional semantic vectors) and it is one of the models used in [ConceptNet Numberbatch](https://github.com/commonsense/conceptnet-numberbatch).

To add source model (vectors) into a Lucene index
```
CALL ga.nlp.ml.word2vec.addModel(<path_to_source_dir>, <path_to_index>, <identifier>)
```
* `<path_to_source_dir>` is a full path to the directory with source vectors to be indexed
* `<path_to_index>` is a full path where the index will be stored
* `<identifier>` is a custom string that uniquely identifies the model

To list available models:
```
CALL ga.nlp.ml.word2vec.listModels
```

The model can now be used to compute cosine similarities between words:
```
WITH ga.nlp.ml.word2vec.wordVector('äpple', 'swedish-numberbatch') AS appleVector,
ga.nlp.ml.word2vec.wordVector('frukt', 'swedish-numberbatch') AS fruitVector
RETURN ga.nlp.ml.similarity.cosine(appleVector, fruitVector) AS similarity
```
* 1st parameter: word
* 2nd parameter: model identifier

Or you can ask directly for a word2vec of a node which has a word stored in property `value`:
```
MATCH (n1:Tag), (n2:Tag)
WHERE ...
WITH ga.nlp.ml.word2vec.vector(n1, <model_name>) AS vector1,
ga.nlp.ml.word2vec.vector(n2, <model_name>) AS vector2
RETURN ga.nlp.ml.similarity.cosine(vector1, vector2) AS similarity
```

We can also permanently store the word2vec vectors to Tag nodes:
```
CALL ga.nlp.ml.word2vec.attach({query:'MATCH (t:Tag) RETURN t', modelName:'swedish-numberbatch'})
```
* `query`: query which returns tags to which embedding vectors should be attached
* `modelName`: model to use

You can also get the nearest neighbors with the following procedure :

```
CALL ga.nlp.ml.word2vec.nn('analyzed', 10, 'fasttext') YIELD word, distance RETURN word, distance
```

For large models, for example full fasttext for english, approximately 2 million words, it will be inefficient to compute the nearest neighbors on the fly.

You can load the model into memory in order to have faster nearest neighbors ( fasttext 1M word vectors generally takes 27 seconds if needed to read from disk, ~300ms in memory) :

Make sure to have efficient heap memory dedicated to Neo4j :

```
dbms.memory.heap.initial_size=3000m
dbms.memory.heap.max_size=5000m
```

Load the model into memory :

```
CALL ga.nlp.ml.word2vec.load(<maxNeighbors>, <modelName>)
```

And retrieve it with

```
CALL ga.nlp.ml.word2vec.nn(<word>,<maxNeighbors>,<modelName>)
```

#### Using other models

You can use any word embedding model as long as the following is true :

- Every line contain the word + the vector
- The file has a `.txt` extension

For example, you can load the models from fasttext and just rename the file from `.vec` to `.txt` : https://fasttext.cc/docs/en/english-vectors.html

### Parsing PDF Documents

```
CALL ga.nlp.parser.pdf("file:///Users/ikwattro/_graphs/nlp/import/myfile.pdf") YIELD number, paragraphs
```

The procedure return rows with columns `number` being the page number and `paragraphs` being a `List<String>` of paragraph texts.

You can also pass an `http` or `https` url to the procedure for loading a file from a remote location.

#### Exclude content from the pdf

In some cases, pdf documents have some recurrent useless content like page footers etc, you can excluded them from the parsing by
passing a list of regexes defining the parts to exclude :

```
CALL ga.nlp.parser.pdf("myfile.pdf", ["^[0-9]$","^Licensed to"])
```

#### Use a different user Agent than TIKA

TIKA can be recognized as crawler and be denied access to some sites containing pdf's. You can override this by passing a `UserAgent` option :

```
CALL ga.nlp.parser.pdf($url, [], {UserAgent: 'Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.7.2) Gecko/20040803'})
```

### Extras

#### Parsing raw content from a file

```
RETURN ga.nlp.parse.raw(<path-to-file>) AS content
```

#### Storing only certain Tag/Tokens

In certain situations, it would be useful to store only certain values instead of the full graph, note though that it might reduce the ability to extract insights ( textRank ) for eg :

```
CALL ga.nlp.processor.addPipeline({
name:"whitelist",
whitelist:"hello,john,ibm",
textProcessor:"com.graphaware.nlp.enterprise.processor.EnterpriseStanfordTextProcessor",
processingSteps:{tokenize:true, ner:true}})
```

```
CALL ga.nlp.annotate({text:"Hello, my name is John and I worked at IBM.", id:"test-123", pipeline:"whitelist", checkLanguage:false})
YIELD result
RETURN result
```

### Parsing WebVTT

WebVTT is the format for Web Video Text Tracks, such as Youtube Transcripts of videos : https://fr.wikipedia.org/wiki/WebVTT

```
CALL ga.nlp.parser.webvtt("url-to-transcript.vtt") YIELD startTime, endTime, text
```

### Listing files from directory(ies)

```
CALL ga.nlp.utils.listFiles(<path-to-directory>, <extensionFilter>)

// eg:

CALL ga.nlp.utils.listFiles("/Users/ikwattro/dev/papers", ".pdf") YIELD filePath RETURN filePath
```

The above procedure list files of the current directory only, if you need to walk the children directories as well, use `walkdir` :

```
CALL ga.nlp.utils.walkdir("/Users/ikwattro/dev/papers", ".pdf") YIELD filePath RETURN filePath
```

## Additional Procedures

### ga.nlp.config.model.list()

List stored models and their paths

### ga.nlp.refreshPipeline(<name>)

Remove and re-create a pipeline with the same configuration ( useful when using static ner files that have been changed for eg )


## License

Copyright (c) 2013-2019 GraphAware

GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program.
If not, see <http://www.gnu.org/licenses/>.
