3.4.6.52.13-SNAPSHOT

- TagOccurrence nodes now receive the NE_$$$ label
- TextRank persistence is now decouple from computation
- Persistence can now store OptimizedCoref from NLP-EE
- Ability to listen to Annotation Events before it is stored
- AnnotatedText object is now serializable and deserializable to json string

3.4.6.52.12

- Added the ability to whitelist only certain words in a pipeline
- Fixed an issue with the PDF Parser not returning the first page
- Added the ability to parse WebVTT file formats
- Added `utils.listFiles` and `utils.walkdir` procedures
- Added the ability to pass a custom UserAgent for TIKA