/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.processor;


public class TextProcessorProcedure {
//
//    private static final Logger LOG = LoggerFactory.getLogger(TextProcessorProcedure.class);
//    public static final String SUCCESS = "success";
//
//    private String defaultTextProcessorName;
//    private final TextProcessor textProcessor;
//    private final GraphDatabaseService database;
//    private final TextProcessorsManager processorManager;
//
//    private static final String PARAMETER_NAME_TEXT = "text";
//    private static final String PARAMETER_NAME_FILTER = "filter";
//    private static final String PARAMETER_NAME_ANNOTATED_TEXT = "node";
//    private static final String PARAMETER_NAME_ID = "id";
//    private static final String PARAMETER_NAME_DEEP_LEVEL = "nlpDepth";
//    private static final String PARAMETER_NAME_STORE_TEXT = "store";
//    private static final String PARAMETER_NAME_LANGUAGE_CHECK = "languageCheck";
//    private static final String PARAMETER_NAME_OUTPUT_TP_CLASS = "class";
//
//    private static final String PARAMETER_NAME_TRAIN_PROJECT = "project";
//    private static final String PARAMETER_NAME_TRAIN_ALG = "alg";
//    private static final String PARAMETER_NAME_TRAIN_MODEL = "model";
//    private static final String PARAMETER_NAME_TRAIN_FILE = "file";
//    private static final String PARAMETER_NAME_TRAIN_LANG = "lang";
//    private static final List<String> TRAINING_PARAMETERS = Arrays.asList(GenericModelParameters.TRAIN_ALG, GenericModelParameters.TRAIN_TYPE, GenericModelParameters.TRAIN_ITER, GenericModelParameters.TRAIN_CUTOFF,
//            GenericModelParameters.TRAIN_THREADS, GenericModelParameters.TRAIN_ENTITYTYPE, GenericModelParameters.VALIDATE_FOLDS, GenericModelParameters.VALIDATE_FILE);
//
//    private static final String PARAMETER_NAME_FORCE = "force";
//
//    public TextProcessorProcedure(GraphDatabaseService database, TextProcessorsManager processorManager) {
//        this.database = database;
//        this.processorManager = processorManager;
//
//        this.defaultTextProcessorName = processorManager.getDefaultProcessorName();
//        this.textProcessor = processorManager.getDefaultProcessor();
//        if (this.textProcessor == null) {
//            LOG.warn("Extraction of the default text processor (" + this.defaultTextProcessorName + ") failed.");
//        }
//    }
//
//

//
//    public CallableProcedure.BasicProcedure train() {
//        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("train"))
//                .mode(Mode.WRITE)
//                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
//                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTString)
//                .build()) {
//
//            @Override
//            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
//                checkIsMap(input[0]);
//                Map<String, Object> inputParams = (Map) input[0];
//
//                // mandatory arguments
//                if (!inputParams.containsKey(PARAMETER_NAME_TRAIN_ALG)
//                        || !inputParams.containsKey(PARAMETER_NAME_TRAIN_MODEL)
//                        || !inputParams.containsKey(PARAMETER_NAME_TRAIN_FILE)) {
//                    throw new RuntimeException("You need to specify mandatory parameters: " + PARAMETER_NAME_TRAIN_ALG + ", " + PARAMETER_NAME_TRAIN_MODEL + ", " + PARAMETER_NAME_TRAIN_FILE);
//                }
//                String alg = String.valueOf(inputParams.get(PARAMETER_NAME_TRAIN_ALG));
//                String model = String.valueOf(inputParams.get(PARAMETER_NAME_TRAIN_MODEL));
//                String file = String.valueOf(inputParams.get(PARAMETER_NAME_TRAIN_FILE));
//
//                // optional arguments
//                String project = String.valueOf(inputParams.getOrDefault(PARAMETER_NAME_TRAIN_PROJECT, "default"));
//                String lang = String.valueOf(inputParams.getOrDefault(PARAMETER_NAME_TRAIN_LANG, "en"));
//
//                // training parameters (optional)
//                Map<String, String> params = new HashMap<String, String>();
//                TRAINING_PARAMETERS.forEach(par -> {
//                    if (inputParams.containsKey(par)) {
//                        params.put(par, String.valueOf(inputParams.get(par)));
//                    }
//                });
//
//                // check training parameters consistency: are there some unexpected keys? (possible typos)
//                List<String> unusedKeys = new ArrayList<String>();
//                List<String> otherKeys = Arrays.asList(PARAMETER_NAME_TRAIN_ALG, PARAMETER_NAME_TRAIN_MODEL, PARAMETER_NAME_TRAIN_FILE, PARAMETER_NAME_TRAIN_PROJECT, PARAMETER_NAME_TRAIN_LANG);
//                inputParams.forEach((k, v) -> {
//                    if (!TRAINING_PARAMETERS.contains(k) && !otherKeys.contains(k)) {
//                        unusedKeys.add(k);
//                    }
//                });
//                if (unusedKeys.size() > 0) {
//                    LOG.warn("Warning! Unused parameter(s) (possible typos?): " + String.join(", ", unusedKeys));
//                }
//
//                TextProcessor currentTP = retrieveTextProcessor(inputParams, "");
//
//                String res = currentTP.train(project, alg, model, file, lang, params);
//
//                if (res.length() > 0) {
//                    res = "success: " + res;
//                } else {
//                    res = "failure";
//                }
//
//                if (unusedKeys.size() > 0) {
//                    res += "; Warning, unsed parameter(s): " + String.join(", ", unusedKeys);
//                }
//
//                return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{res}).iterator());
//            }
//        };
//    }
//
//    public CallableProcedure.BasicProcedure test() {
//        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("test"))
//                .mode(Mode.WRITE)
//                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
//                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTString)
//                .build()) {
//
//            @Override
//            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
//                checkIsMap(input[0]);
//                Map<String, Object> inputParams = (Map) input[0];
//
//                // mandatory arguments
//                if (!inputParams.containsKey(PARAMETER_NAME_TRAIN_ALG)
//                        || !inputParams.containsKey(PARAMETER_NAME_TRAIN_MODEL)
//                        || !inputParams.containsKey(PARAMETER_NAME_TRAIN_FILE)) {
//                    throw new RuntimeException("You need to specify mandatory parameters: " + PARAMETER_NAME_TRAIN_ALG + ", " + PARAMETER_NAME_TRAIN_MODEL + ", " + PARAMETER_NAME_TRAIN_FILE);
//                }
//                String alg = String.valueOf(inputParams.get(PARAMETER_NAME_TRAIN_ALG));
//                String model = String.valueOf(inputParams.get(PARAMETER_NAME_TRAIN_MODEL));
//                String file = String.valueOf(inputParams.get(PARAMETER_NAME_TRAIN_FILE));
//
//                // optional arguments
//                String project = String.valueOf(inputParams.getOrDefault(PARAMETER_NAME_TRAIN_PROJECT, "default"));
//                String lang = String.valueOf(inputParams.getOrDefault(PARAMETER_NAME_TRAIN_LANG, "en"));
//
//                TextProcessor currentTP = retrieveTextProcessor(inputParams, "");
//
//                String res = currentTP.test(project, alg, model, file, lang);
//
//                if (res.length() > 0) {
//                    res = "success: " + res;
//                } else {
//                    res = "failure";
//                }
//
//                return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{res}).iterator());
//            }
//        };
//    }
//
}
