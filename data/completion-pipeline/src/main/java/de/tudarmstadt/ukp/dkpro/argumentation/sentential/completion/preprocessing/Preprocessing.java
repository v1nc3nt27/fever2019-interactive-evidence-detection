package de.tudarmstadt.ukp.dkpro.argumentation.sentential.completion.preprocessing;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import java.util.Collection;
import java.util.HashMap;

import de.tudarmstadt.ukp.cst.ConArgument;
import de.tudarmstadt.ukp.cst.ProArgument;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.tudarmstadt.ukp.cst.Argument;

public class Preprocessing {

	/**
	 * This method takes texts, a string and the language, and returns a hashmap of
	 * sentence hash as keys and tokenized sentences as values
	 * 
	 * @param content
	 * @param lang
	 * @param hashes
	 * @param hashesContra
	 * @return
	 */
	public static HashMap<String, String> extractSentences(String content, String lang, String filename, String hashesPro, String hashesContra) {
		HashMap<String, String> retrievedSentences = new HashMap<String, String>();

		try {
            JCas jcas = JCasFactory.createJCas("desc.types.ArgumentsTypeSystem", "desc.type.metadata_customized", "desc.type.LexicalUnits");
			jcas.setDocumentText(content);
			DocumentMetaData meta = DocumentMetaData.create(jcas);
			meta.setLanguage(lang);

			// preprocessing
			AnalysisEngineDescription preprocessing = createEngineDescription(StanfordSegmenter.class) ;
			AnalysisEngineDescription annotator = createEngineDescription(SentenceAnnotator.class,
					SentenceAnnotator.PARAM_ANNOTATION_PRO_CLASS_NAME, ProArgument.class.getName(),
					SentenceAnnotator.PARAM_ANNOTATION_CON_CLASS_NAME, ConArgument.class.getName(),
					SentenceAnnotator.PARAM_HASHES_PRO, hashesPro, SentenceAnnotator.PARAM_HASHES_CONTRA, hashesContra);

			AnalysisEngineDescription writer = createEngineDescription(LabeledSentenceWriter.class, LabeledSentenceWriter.PARAM_FILE_NAME, filename, LabeledSentenceWriter.PARAM_LABEL_TYPE_PRO, ProArgument.class.getName(), LabeledSentenceWriter.PARAM_LABEL_TYPE_CON, ConArgument.class.getName());
			// run Pipeline
			runPipeline(jcas, preprocessing, annotator, writer);

			// collect sentences
			Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return retrievedSentences;
	}

}
