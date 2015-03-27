package nlp.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import erTagger.ERTagger;
import erTagger.ErdBuilder;
import trie.LeafNode;
import trie.Lookup;
import trie.Trie;
import util.Tuple;

public class Paragraph {

	private Sentences Paragraph;
	private Model ParagraphDataModel;

	
	/**
	 * Loads the paragraph provided from paragraph into the list of sentences.
	 * POSTags the sentences.
	 * 
	 * @param paragraph
	 */
	public Paragraph(String paragraph) {
		this.Paragraph = new Sentences();
		load(paragraph);
	}

	private void load(String paragraphText) {
		ArrayList<Sentence> sentences = new ArrayList<Sentence>();
		for (String sentenceValue : Arrays.asList(paragraphText.split("\\."))) {
			String currentSentence = sentenceValue + ".";
			Sentence sent = new Sentence(currentSentence);
			sentences.add(sent);
		}
		this.Paragraph.setSentence(sentences);
	}

	public void acquireDataModel(Trie trie) {
		for (Sentence sentence : this.Paragraph.getSentence()) {

			ERTagger.LOGGER.info(String.format("Acquiring Data Model for: %s",
					sentence.getValue()));

			LeafNode leafInfo = Lookup.lookup(trie, sentence, new Tuple(100, 80));
			if (leafInfo == null) {
				ERTagger.LOGGER.severe("Lookup Permanently Failed.");
			} else {
				ERTagger.LOGGER.info("Data Model Acquired.");
				sentence.setDataModel(leafInfo.getDataModel());
			}
		}
		createDataModel();
	}

	private void createDataModel() {
		List<Entity> entities = new LinkedList<Entity>();
		List<Relationship> relationships = new LinkedList<Relationship>();

		for (Sentence sentence : this.Paragraph.getSentence()) {
			entities.addAll(sentence.getDataModel().getEntities());
			relationships.addAll(sentence.getDataModel().getRelationships());
		}
		this.ParagraphDataModel = new Model();
		this.ParagraphDataModel.setEntities(entities);
		this.ParagraphDataModel.setRelationships(relationships);

		mergeDuplicateEntities();
		mergeDuplicateRelationships();

		updateIds();
	}

	private void mergeDuplicateRelationships() {
		Set<Relationship> relationSet = new HashSet<Relationship>();

		for (Relationship relationship : this.ParagraphDataModel
				.getRelationships()) {
			boolean result = relationSet.add(relationship);
			if (result == false) {
				ERTagger.LOGGER.info("Duplicate Relationship removed.");
			}
		}
		List<Relationship> newRelationList = new ArrayList<Relationship>(
				relationSet.size());
		newRelationList.addAll(relationSet);
		this.ParagraphDataModel.setRelationships(newRelationList);
	}

	private void mergeDuplicateEntities() {
		List<Entity> entities = this.ParagraphDataModel.getEntities();
		Integer entitiesSize = entities.size();

		for (int i = 0; i < entitiesSize; i++) {
			Entity current = entities.get(i);

			for (int j = i + 1; j < entitiesSize; j++) {
				if (current.getLemmName().compareTo(
						entities.get(j).getLemmName()) == 0) {
					/* Merge Entities */
					Entity duplicateName = entities.get(j);
					Set<Attribute> attributeSet = new HashSet<Attribute>();

					attributeSet.addAll(current.getAttributes());
					attributeSet.addAll(duplicateName.getAttributes());

					List<Attribute> newAttributeList = new ArrayList<Attribute>();
					newAttributeList.addAll(attributeSet);

					current.setAttributes(newAttributeList);

					entities.remove(j);
					j = j - 1; // since an element from array is removed.
					entitiesSize = entitiesSize - 1;
				}
			}
		}
		this.ParagraphDataModel.setEntities(entities);
	}

	// FIXME Incomplete Code Here.
	private boolean isPromotable(Attribute attribute) {
		Iterator<Entity> entityItr = this.ParagraphDataModel.getEntities()
				.iterator();
		while (entityItr.hasNext()) {
			Entity entity = entityItr.next();
			if (entity.getName().compareTo(attribute.getName()) == 0) {
				return true;
			}
		}
		return false;
	}

	private void updateIds() {
		int idIndex = 1; // common id inedex for entities and relationships
		for (Entity entity : this.ParagraphDataModel.getEntities()) {
			entity.setId(idIndex);
			idIndex++;
		}
		for (Relationship relationship : this.ParagraphDataModel
				.getRelationships()) {
			relationship.setId(idIndex);
			idIndex++;
		}
	}

	/**
	 * Converts the List of sentences into a xml format which can be read by the
	 * plugin.
	 * 
	 * IMPLEMENTATION INCOMPLETE
	 * 
	 * @return XML in string format.
	 */
	public String toErdXml(File outputFile) {
		// TODO Implement this.
		return null;
	}

	/**
	 * Saves the xml as generated by toErdXml to a path specified by filepath
	 * 
	 * @param filepath
	 *            Absolute path of file to store the file. Provide the extension
	 *            of the file.
	 */
	public void saveAsXml(String outputFilepath) {
		try {
			ErdBuilder erdb = new ErdBuilder(new File(outputFilepath));
			erdb.parse(this.ParagraphDataModel);
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException te) {
			te.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return this.Paragraph.toString();
	}

	public Model getParagraphDataModel() {
		return this.ParagraphDataModel;
	}
}