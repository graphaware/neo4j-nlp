GraphAware Natural Language Processing
=========================================================

This plugin add NLP functionalities to Neo4j

List of procedures available:

1. Tag extraction 

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

2. Sentiment extraction

This procedure allows to process sentences beloging to AnnotatedText node and add sentiment to each of them. A label will be asigned to the sentence from VeryNegative, Negative, Neutral, Positive, VeryPositive

ga.nlp.sentiment

3. Ontology

ga.nlp.concept

4. Search

ga.nlp.search

5. Language Detection

ga.nlp.language

6. NLP based filter

ga.nlp.filter

7. Cosine similarity computation

ga.nlp.cosine.compute
