package trie;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import nlp.objects.Sentence;
import nlp.objects.Sentences;
import nlp.objects.Word;

/**
 * Trie:
 * 
 * @author Suresh Sarda
 *
 */
public class Trie {
	/* Default print behavior to TAGS only */
	private PrintDetail PrintBehavior = PrintDetail.TAGS_ONLY;
	private List<Node> Root;
	private boolean StatsCollected = false;

	public enum PrintDetail {
		TAGS_ONLY, //prints only POST of the trie
		TAGS_AND_PROBABILITY //prints POSTs as well as the probability of that node
	};

	
	/**
	 * Default constructor. Initializes all the elements.
	 */
	public Trie() {
		Root = new ArrayList<Node>();
	}
	
	public boolean isStatsCollected() {
		return StatsCollected;
	}

	public void setStatsCollected(boolean statsCollected) {
		StatsCollected = statsCollected;
	}
	/**
	 * Insert an array of sentences in the Trie. Array of sentence --> Sentences
	 * class
	 * 
	 * @param trainingData
	 *            The list of sentences encapsulated in Sentences type.
	 */
	public void insertIntoTrie(Sentences trainingData) {
		/*
		 * For each sentence in the list, Insert into the trie if it doesn't
		 * exist already.
		 */
		for (Sentence sentence : trainingData.getSentence()) {
			insertIntoTrie(sentence);
		}
	}

	/**
	 * Insert a sentence into the Trie.
	 * 
	 * @param sentence
	 *            The sentence to be added.
	 */
	private void insertIntoTrie(Sentence sentence) {
		/* Search if the root contains */
		Iterator<Node> rootIterator = Root.iterator();
		Node parent = null;

		/* Search if the the branch exists in root */
		while (rootIterator.hasNext()) {
			Node current = rootIterator.next();
			if (current.getTag().compareTo(
					sentence.getTokens().get(0).getPost()) == 0) {
				parent = current;
				break;
			}
		}
		/*
		 * if parent equals null -> nothing found in the root. Start a new
		 * branch
		 */
		if (parent == null) {
			Word first = sentence.getTokens().get(0);
			Node node = new Node(first.getPost(), first.getName());
			Root.add(node);
			insertRemaining(sentence, 1, node);
		} else {
			/* Branch already exists, update */
			Iterator<Word> wordIterator = sentence.getTokens().iterator();

			@SuppressWarnings("unused")
			Word first = wordIterator.next();
			while (wordIterator.hasNext()) {
				Word currentWord = wordIterator.next();
				Node found = parent.findChild(currentWord.getPost());
				if (found != null)
					parent = found;
				else {
					insertRemaining(sentence,
							sentence.getTokens().indexOf(currentWord), parent);
					break;
				}
			}
		}
	}

	/**
	 * Insert all the remaining tags of the sentence in trie.
	 * 
	 * @param sentence
	 *            Sentence from which all the tags are to be put in the trie
	 * @param position
	 *            Position from where to start
	 * @param parent
	 *            Continue here in the Trie
	 */
	private void insertRemaining(Sentence sentence, int position, Node parent) {
		for (int i = position; i < sentence.getTokens().size(); i++) {
			Word word = sentence.getTokens().get(i);
			Node child = new Node(word.getPost(), word.getName());
			parent.addChild(child);
			parent = child;
		}
		/* Insert leaf information of the sentence */
		parent.setLeafInformation(sentence.getDataModel());
	}

	/**
	 * Implementation Pending Look for a sequence in the Trie. Match the closest
	 * possible option and return.
	 * 
	 * @param sentence
	 *            The sentence the look for.
	 * @return
	 */
	public LeafNode lookup(Sentence sentence) {
		/*
		 * Game-plan: Search the Trie for exact match If ExactMatch not found,
		 * Search by removing stop words - Use AdvancedNodeSkip Algorithm If not
		 * found, calculate EditDistance and find the nearest branch Give the
		 * DataModel of the nearest match.
		 */
		int wordIndex = 0;
		List<Word> tokens = sentence.getTokens();
		for (Node node : Root) {
			if (node.getTag().compareTo(tokens.get(wordIndex).getPost()) == 0) {
				LeafNode leafInfo = checkExactMatch(node, sentence);
				if (leafInfo == null)
					break;
				else
					return leafInfo;
			}
		}
		LeafNode leafInfo = advancedNodeSkip(Root, sentence);
		if (leafInfo == null) {
			System.err.println("No match found in the trie.");
			return null;
		} else {
			return leafInfo;
		}
	}

	private LeafNode checkExactMatch(Node parent, Sentence sentence) {

		Iterator<Word> word = sentence.getTokens().iterator();
		word.next(); // skip the first word

		while (true) {
			Word currentWord = word.next();

			for (Node child : parent.getChildren()) {
				if (child.getTag().compareTo(currentWord.getPost()) == 0) {
					parent = child;
					if (word.hasNext() == true) {
						// more words to compare
						continue;
					} else {
						// this was the last word
						return child.getLeafInformation();
					}
				}
			}
			return null;
		}
	}

	private LeafNode advancedNodeSkip(List<Node> Root, Sentence sentence) {
		int wordIndex = 0;
		List<Word> tokens = sentence.getTokens();
		Stack<Node> removedStopWord = new Stack<Node>();
		List<Node> test = probableRoutes(Root);

		for (Node node : test) {
			removedStopWord.push(node);
		}
		return null;
	}

	/**
	 * Finds probable routes from the parent nodes after trying to skip the stop words.
	 * @param parents
	 * @return
	 */
	private List<Node> probableRoutes(List<Node> parents) {
		List<Node> probablePaths = new ArrayList<Node>();
		for (Node parent : parents) {
		/* FIXME
		 * It only tries to skip the unwanted nodes. It should be able to skip stop word tags
		 * as well as consider them if required by test sentence.
		 * 
		 * For now it only skips all the possible stop words.
		 */
			if (parent.getIsStopWordProbability() <= 0.5) {
				probablePaths.add(parent);
				return probablePaths;
			}
			probablePaths.addAll(probableRoutes(parent.getChildren()));
		}
		return null;
	}

	/*
	 * Trie print functions
	 */

	/**
	 * Analyze the Trie and print the output to std out Analuzes and outputs
	 * following: 1. For tags - a. Number of times they were used for ER tagging
	 * and number of times they were not b. Number of tags which have always
	 * been used for stop words 2. For Words - Number of words used in ER tags.
	 * This will show how many words were never used and will help update the
	 * list of stopwords.
	 * 
	 * Usage: Train the trie with some sentences using InsertIntoTrie() and call
	 * this function to produce the output.
	 * 
	 * @param outStream
	 *            : The output stream where the results are to be printed.
	 */
	public void analyze(PrintStream printer) {
		// TODO Implementation Pending*/
	}

	/**
	 * DO NOT USE THIS FUNCTION.
	 */
	@Override
	public String toString() {
		// FIXME Output is not formatted properly.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		print(ps);
		try {
			return baos.toString("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Display the Trie graphically on the given PrintStream
	 * 
	 * @param printer
	 *            The PrintStream to print the output
	 * 
	 */
	public void print(PrintStream printer) {
		for (Node node : Root) {
			printer.println();
			traverseAndPrint(node, 0, printer, this.PrintBehavior);
		}
	}

	/**
	 * Display the Trie graphically on the given PrintStream
	 * 
	 * @param printer
	 *            The PrintStream to print the output.
	 * @param printDetail
	 *            The level of detail to print as specified by the enum
	 *            PrintDetail. This value will be set as default behavior and
	 *            used in all subsequent calls unless changed otherwise.
	 */
	public void print(PrintStream printer, PrintDetail printDetail) {
		PrintBehavior = printDetail;
		print(printer);
	}

	/**
	 * The format in which every node will be printed when Print function is
	 * called.
	 * 
	 * @param node
	 *            The node into consideration
	 */
	private void printNode(Node node, PrintStream printer,
			PrintDetail printDetail) {
		switch (printDetail) {
		case TAGS_ONLY:
			printer.printf("%-5s", node.getTag());
			break;
		case TAGS_AND_PROBABILITY:
			System.out.printf("%-5s(%1.2f) ", node.getTag(),
					node.getIsStopWordProbability()); // print with probability
			break;
		default:
			System.err.println("Invalid PrintDetail flag.");
			break;
		}

	}

	private void traverseAndPrint(Node node, int level, PrintStream printer,
			PrintDetail printDetail) {
		// OffsetToLevel(level);
		while (node.getChildren().get(0) != null) {
			level++;
			printNode(node, printer, printDetail);
			if (node.getChildren().size() > 1) {
				for (int i = 1; i < node.getChildren().size(); i++) {
					traverseAndPrint(node.getChildren().get(i), level, printer,
							printDetail);
					offsetToLevel(level, printer, printDetail);
				}
			}
			node = node.getChildren().get(0);
			if (node.getChildren().isEmpty() == true) {
				/* No more children to mine. Print and break */
				printNode(node, printer, printDetail);
				break;
			}
		}
	}

	private void offsetToLevel(int level, PrintStream printer,
			PrintDetail printDetail) {
		printer.println();
		for (int i = 0; i < level; i++) {
			switch (printDetail) {
			case TAGS_ONLY:
				System.out.printf("%-5s", "|");
				break;
			case TAGS_AND_PROBABILITY:
				System.out.printf("%-12s", "|");
				break;
			default: /* Invalid PrintDetail Information */
				System.err.println("Invalid PrintDetail Flag.");
				break;
			}
		}
	}
}