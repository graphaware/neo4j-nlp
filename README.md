GraphAware Natural Language Processing
=========================================================

This plugin add NLP functionalities to Neo4j. It requires the <a href="https://github.com/graphaware/neo4j-framework" target="_blank">GraphAware Neo4j Framework</a> and its NLP methods are implemented either using the <a href="https://github.com/graphaware/neo4j-nlp-stanfordnlp" target="_blank">StanfordNLP</a> or <a href="https://github.com/graphaware/neo4j-nlp-opennlp" target="_blank">OpenNLP</a>.

Getting the Software
-------------------------
### Server Mode
When using Neo4j in the <a href="http://docs.neo4j.org/chunked/stable/server-installation.html" target="_blank">standalone server</a> mode, you will need the <a href="https://github.com/graphaware/neo4j-framework" target="_blank">GraphAware Neo4j Framework</a> and <a href="https://github.com/graphaware/neo4j-nlp" target="_blank">GraphAware NLP</a> .jar files (both of which you can download <a href="https://products.graphaware.com">here</a>) dropped into the `plugins/` directory of your Neo4j installation. 

The following needs to be appended to the `neo4j.conf` file in the `config/` directory:

```
  dbms.unmanaged_extension_classes=com.graphaware.server=/graphaware
  com.graphaware.runtime.enabled=true

  com.graphaware.module.NLP.2=com.graphaware.nlp.module.NLPBootstrapper
```

The actual implementation of the NLP tools is in packages <a href="https://github.com/graphaware/neo4j-nlp-stanfordnlp" target="_blank">StanfordNLP</a> and <a href="https://github.com/graphaware/neo4j-nlp-opennlp" target="_blank">OpenNLP</a> (both provide similar functionalities described in section Getting Started). To get them working, just compile them and drop the .jar file(s) into `plugins/` directory:

```
# First you need to install neo4j-nlp
cd neo4j-nlp
mvn clean install

# Next you can proceed to the OpenNLP and StanfordNLP
cd ../neo4j-nlp-opennlp
mvn clean package
cp target/nlp-stanfordnlp-1.0.0-SNAPSHOT.jar <YOUR_NEO4J_DIR>/plugins
```

Example of the `plugins/` directory:
```
-rw-r--r--  1 abc  staff   6108799 May 16 11:27 graphaware-nlp-1.0-SNAPSHOT.jar
-rw-r--r--@ 1 abc  staff  13391931 May  5 09:34 graphaware-server-enterprise-all-3.1.3.47.jar
-rw-r--r--  1 abc  staff  46678477 May 16 14:59 nlp-opennlp-1.0-SNAPSHOT.jar
```

Note: both implementations (especially StanfordNLP) are rather greedy - you'll need a lot of RAM.

Getting Started
--------------------

List of procedures available:

##1. Tag extraction 

This procedure allows to process text and get back sentences and tags after processing. The return value is a node of type AnnotatedText that is connected to sentences and them to tags.
This is an example of usage:

```
#Add a new node with the text (not mandatory)
CREATE (news:News {text:"Scores of people were already lying dead or injured inside a crowded Orlando nightclub,
    and the police had spent hours trying to connect with the gunman and end the situation without further violence.
    But when Omar Mateen threatened to set off explosives, the police decided to act, and pushed their way through a
    wall to end the bloody standoff."}) 
RETURN news;

#Annotate the news
MATCH (n:News)
CALL ga.nlp.annotate({text:n.text, id: n.uuid}) YIELD result
MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)
RETURN n, result;
```

Available parameters are:

* text: (mandatory) the text to be processed
* id: (mandatory) the id to assign to the new node created
* sentiment: (optional, default false) this allow to specify if also sentiment is extracted for each sentence in text, if true a label will be asigned to the sentence from VeryNegative, Negative, Neutral, Positive, VeryPositive
* store: (optional, default true) this enable the storing of sentence in the sentence node. This is necessary if sentences need to be processed later, for example for sentiment extraction

##2. Sentiment extraction

This procedure allows to process sentences beloging to AnnotatedText node and add sentiment to each of them. A label will be asigned to the sentence from VeryNegative, Negative, Neutral, Positive, VeryPositive

```
MATCH (a:AnnotatedText {id: {id}}) WITH a 
CALL ga.nlp.sentiment({node:a}) YIELD result 
MATCH (result)-[:CONTAINS_SENTENCE]->(s:Sentence) 
return labels(s) as labels
```

##3. Ontology
Another feature provided by GraphAware NLP is the ability to build ontology hierarchies, starting from the tags extracted from the text. 
The source for this ontology is ConceptNet5. It is a semantic network containing lots of things computers know about the world, 
especially when understanding text written by people.

```
MATCH (a:AnnotatedText)
CALL ga.nlp.concept({node:a, depth: 2}) YIELD result
return result
```

##4. Search
Text processed during the annotation process is decomposed in all the main tags. Stop words, lemmatization, punctuation pruning and other cleaning procedures are applied to reduce the amount of tags to the most significant. 
Furthermore, for each tag, its term frequency is stored to provide information about how often a lemma appears in the document. 
Using such data and inspired by Elasticsearch scoring functions, GraphAware NLP exposes a search procedure that provides basic search capabilities leveraging tag information stored after text analysis.

```
CALL ga.nlp.search("gun Orlando") YIELD result, score
MATCH (result)<-[]-(news:News)
RETURN DISTINCT news, score
ORDER BY score desc;
```

##5. Language Detection

```
CALL ga.nlp.language({text:{value}}) YIELD result return result
```

##6. NLP based filter

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
    the outcome of the election was exactly as we predicted.", filter: 'Owen Bennett Jones/PERSON, BBC, Pakistan/LOCATION'}) YIELD result 
return result
```

##7. Cosine similarity computation
Once tags are extracted from all the news or other nodes containing some text, it is possible to compute similarities between them using content based similarity. 
During this process, each annotated text is described using the TF-IDF encoding format. TF-IDF is an established technique from the field of information retrieval and stands for Term Frequency-Inverse Document Frequency. 
Text documents can be TF-IDF encoded as vectors in a multidimensional Euclidean space. The space dimensions correspond to the tags, previously extracted from the documents. The coordinates of a given document in each dimension (i.e., for each tag) are calculated as a product of two sub-measures: term frequency and inverse document frequency.

```
CALL ga.nlp.cosine.compute({}) YIELD result
```

