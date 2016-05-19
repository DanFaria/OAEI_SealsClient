package eu.sealsproject.omt.client.interactive;

import eu.sealsproject.omt.client.Relation;

/**
 * A Mapping between two ontology entities, given by
 * their URIs, with a given Relation
 * 
 * @author Daniel Faria
 */

public class Mapping
{
	private String sourceURI;
	private String targetURI;
	private Relation rel;
	
	/**
	 * Constructs a new Mapping between the given source and target URIs
	 * with the given relation
	 * @param sourceURI: the URI of the source entity of the Mapping
	 * @param targetURI: the URI of the target entity of the Mapping
	 * @param relation: the relation between source and target in the Mapping
	 * in String form ("=", ">", "<")
	 */
	public Mapping(String sourceURI, String targetURI, String relation)
	{
		this.sourceURI = sourceURI;
		this.targetURI = targetURI;
		rel = Relation.parse(relation);
	}

	/**
	 * Constructs a new Mapping between the given source and target URIs
	 * with the given relation
	 * @param sourceURI: the URI of the source entity of the Mapping
	 * @param targetURI: the URI of the target entity of the Mapping
	 * @param rel: the relation between source and target in the Mapping
	 */
	public Mapping(String sourceURI, String targetURI, Relation rel)
	{
		this.sourceURI = sourceURI;
		this.targetURI = targetURI;
		this.rel = rel;
	}
	
	/**
	 * @return the URI of the source entity of the Mapping
	 */
	public String getSourceURI()
	{
		return sourceURI;
	}

	/**
	 * @return the URI of the target entity of the Mapping
	 */
	public String getTargetURI()
	{
		return targetURI;
	}

	/**
	 * @return the relation of the Mapping
	 */
	public Relation getRelation()
	{
		return rel;
	}
}