/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.dkpro.argumentation.sentential.completion.preprocessing;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
//import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UIMA Cas consumer writing a document as labeled documents, with label\tsentence.
 */
@ResourceMetaData(name="Labeled Sentence Writer")
//@MimeTypeCapability({MimeTypes.TEXT_PLAIN})
public class LabeledSentenceWriter extends JCasFileWriter_ImplBase {

    private static final Logger LOGGER = Logger.getLogger(LabeledSentenceWriter.class.toString());

    /**
     * Name of the output file.
     */
    public static final String PARAM_FILE_NAME = "fileName";
    @ConfigurationParameter(name =PARAM_FILE_NAME)
    private String fileName;

    /**
     * Name of the type of annotated sentences.
     */
    public static final String PARAM_LABEL_TYPE_PRO = "labelTypePro";
    @ConfigurationParameter(name = PARAM_LABEL_TYPE_PRO)
    private String labelTypeNamePro;

    public static final String PARAM_LABEL_TYPE_CON = "labelTypeCon";
    @ConfigurationParameter(name = PARAM_LABEL_TYPE_CON)
    private String labelTypeNameCon;

    private Class<? extends Annotation> labelTypePro;
    private Class<? extends Annotation> labelTypeCon;
    private BufferedWriter writer;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        try {
            labelTypePro = (Class<? extends Annotation>) getClass().getClassLoader().loadClass(labelTypeNamePro);
            labelTypeCon = (Class<? extends Annotation>) getClass().getClassLoader().loadClass(labelTypeNameCon);
            writer = new BufferedWriter(new FileWriter(fileName));
        } catch (ClassNotFoundException cnfe) {
            String message = "Could not read class: " + labelTypeNamePro;
            LOGGER.log(Level.SEVERE, message, cnfe);
            throw new ResourceInitializationException(cnfe);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Could not write output file", ioe);
            throw new ResourceInitializationException(ioe);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        try {
            Collection<Sentence> sentences = JCasUtil.select(aJCas, Sentence.class);
            Map<Token, ? extends Collection<? extends Annotation>> coveringPro = JCasUtil.indexCovering(aJCas, Token.class, labelTypePro);
            Map<Token, ? extends Collection<? extends Annotation>> coveringCon = JCasUtil.indexCovering(aJCas, Token.class, labelTypeCon);
            for (Sentence sentence: sentences) {
                String sentenceRow = createSentenceRow(aJCas, coveringPro, coveringCon, sentence);
                writer.write(sentenceRow);
                writer.write("\n");
                LOGGER.info(sentenceRow);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write output file", e);
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        try {
            LOGGER.info("Closing file");
            writer.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not close writer", e);
            throw new AnalysisEngineProcessException(e);
        }
    }

    private String createSentenceRow(JCas aJCas, Map<Token, ? extends Collection<? extends Annotation>> coveringPro, Map<Token, ? extends Collection<? extends Annotation>> coveringCon, Sentence sentence) {
        List<Token> tokens = JCasUtil.selectCovered(aJCas, Token.class, sentence);
        StringBuilder sb = new StringBuilder();
        sb.append("\t");
        boolean isAnnotatedPro = false;
        boolean isAnnotatedCon = false;
        for (Token token: tokens) {
            // if only a single token is annotated, we treat the entire sentence as annotated
            isAnnotatedPro |= !coveringPro.get(token).isEmpty();
            isAnnotatedCon |= !coveringCon.get(token).isEmpty();
            sb.append(token.getCoveredText());
            sb.append(" ");
        }

        String label = "no-annotation";
        if (isAnnotatedPro) {
            label = labelTypePro.getSimpleName();
        }
        if (isAnnotatedCon) {
            label = labelTypeCon.getSimpleName();
        }

        sb.insert(0, label);
        return sb.toString().trim();
    }
}
