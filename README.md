GraphAware Natural Language Processing
=========================================================

This plugin add NLP functionalities to Neo4j

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

3. Ontology
Another feature provided by GraphAware NLP is the ability to build ontology hierarchies, starting from the tags extracted from the text. 
The source for this ontology is ConceptNet5. It is a semantic network containing lots of things computers know about the world, 
especially when understanding text written by people.

```
MATCH (a:AnnotatedText)
CALL ga.nlp.concept({node:a, depth: 2}) YIELD result
return result
```

4. Search
Text processed during the annotation process is decomposed in all the main tags. Stop words, lemmatization, punctuation pruning and other cleaning procedures are applied to reduce the amount of tags to the most significant. 
Furthermore, for each tag, its term frequency is stored to provide information about how often a lemma appears in the document. 
Using such data and inspired by Elasticsearch scoring functions, GraphAware NLP exposes a search procedure that provides basic search capabilities leveraging tag information stored after text analysis.

```
CALL ga.nlp.search("gun Orlando") YIELD result, score
MATCH (result)<-[]-(news:News)
RETURN DISTINCT news, score
ORDER BY score desc;
```

5. Language Detection

```
CALL ga.nlp.language({text:{value}}) YIELD result return result
```

6. NLP based filter

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

7. Cosine similarity computation
Once tags are extracted from all the news or other nodes containing some text, it is possible to compute similarities between them using content based similarity. 
During this process, each annotated text is described using the TF-IDF encoding format. TF-IDF is an established technique from the field of information retrieval and stands for Term Frequency-Inverse Document Frequency. 
Text documents can be TF-IDF encoded as vectors in a multidimensional Euclidean space. The space dimensions correspond to the tags, previously extracted from the documents. The coordinates of a given document in each dimension (i.e., for each tag) are calculated as a product of two sub-measures: term frequency and inverse document frequency.

```
CALL ga.nlp.cosine.compute({}) YIELD result
```

