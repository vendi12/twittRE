package de.mpii.clausie;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.mpii.clausie.Constituent.Type;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;

/** Handles the generation of propositions out of a given clause
 * 
 * @date $ $
 * @version $ $ */
public abstract class PropositionGenerator {
	
	// for corenlp lemmatization SV
	Morphology mph = new Morphology();
    
	ClausIE clausIE;

    /** Relations to be excluded in every constituent of a clause except the verb */
    protected static final Set<GrammaticalRelation> EXCLUDE_RELATIONS;
    
    /** Relations to be excluded in the verb */
    protected static final Set<GrammaticalRelation> EXCLUDE_RELATIONS_VERB;

    static {
        EXCLUDE_RELATIONS = new HashSet<GrammaticalRelation>();
        EXCLUDE_RELATIONS.add(EnglishGrammaticalRelations.RELATIVE_CLAUSE_MODIFIER);
        EXCLUDE_RELATIONS.add(EnglishGrammaticalRelations.APPOSITIONAL_MODIFIER);
        EXCLUDE_RELATIONS.add(EnglishGrammaticalRelations.PARATAXIS);

        EXCLUDE_RELATIONS_VERB = new HashSet<GrammaticalRelation>();
        EXCLUDE_RELATIONS_VERB.addAll(EXCLUDE_RELATIONS);
        EXCLUDE_RELATIONS_VERB.add(EnglishGrammaticalRelations.valueOf("dep")); //without this asome adverbs or auxiliaries will end up in the relation
    }

    /** Constructs a proposition generator*/
    public PropositionGenerator(ClausIE clausIE) {
        this.clausIE = clausIE;
    }

    /** Generates propositions for a given clause*/
    public abstract void generate(List<Proposition> result, Clause clause, List<Boolean> include);

    /** Generates a textual representation of a given constituent plus a set of words*/
    private String generatePhrase(IndexedConstituent constituent, Collection<IndexedWord> words) {
        StringBuffer result = new StringBuffer();
        String separator = "";
        result.append(separator);
//        if (constituent.isPrepositionalPhrase()) {
        
        // lemmatize only verbs SV
    	if (constituent.getType() == Type.VERB && clausIE.options.lemmatize) {
    		IndexedWord baseword = constituent.getRoot();
    		result.append(mph.lemma(baseword.word(), baseword.tag()));
        } else {
	        for (IndexedWord word : words) {
	            result.append(separator);
	            result.append(word.originalText());
	            separator = " ";
	        }
        }
        return result.toString();
    }

    /** Generates a textual representation of a given constituent in a given clause*/
    public String generate(Clause clause, int constituentIndex) {
        Set<GrammaticalRelation> excludeRelations = EXCLUDE_RELATIONS;
        if (clause.verb == constituentIndex) {
            excludeRelations = EXCLUDE_RELATIONS_VERB;
        }
            return generate(clause, constituentIndex, excludeRelations,
                    Collections.<GrammaticalRelation> emptySet());
    }

    /** Generates a textual representation of a given constituent in a given clause*/
    public String generate(Clause clause, int constituentIndex,
            Collection<GrammaticalRelation> excludeRelations,
            Collection<GrammaticalRelation> excludeRelationsTop) {
        Constituent constituent = clause.constituents.get(constituentIndex);
        if (constituent instanceof TextConstituent) {
            return ((TextConstituent) constituent).text();
        } else if (constituent instanceof IndexedConstituent) {
            IndexedConstituent iconstituent = (IndexedConstituent) constituent;
            SemanticGraph subgraph = iconstituent.createReducedSemanticGraph(); 
            DpUtils.removeEdges(subgraph, iconstituent.getRoot(),  
                    excludeRelations, excludeRelationsTop);
            Set<IndexedWord> words = new TreeSet<IndexedWord>(
                    subgraph.descendants(iconstituent.getRoot()));
            for (IndexedWord v : iconstituent.getAdditionalVertexes()) {
                words.addAll(subgraph.descendants(v));
            }
            if (iconstituent.isPrepositionalPhrase())
                words.remove(iconstituent.getRoot());
            return generatePhrase(iconstituent, words);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
