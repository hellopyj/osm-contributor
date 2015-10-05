/**
 * Copyright (C) 2015 eBusiness Information
 *
 * This file is part of OSM Contributor.
 *
 * OSM Contributor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OSM Contributor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OSM Contributor.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.mapsquare.osmcontributor.type.rest;

import java.util.List;

import io.mapsquare.osmcontributor.type.dto.Suggestions;
import io.mapsquare.osmcontributor.type.dto.Combinations;
import io.mapsquare.osmcontributor.type.dto.Wiki;
import retrofit.http.GET;
import retrofit.http.Query;

public interface OsmTagInfoRestClient {

    /**
     * Query for the poitypes suggestions for a given string.
     *
     * @param query The query of the suggestion.
     * @param pageNumber The page number, if null return results of the first page.
     * @param resultPerPage The number of results per page, if null return all the results.
     * @return The suggestions for the given query.
     */
    @GET("/keys/all?qtype=key&sortname=count_ways&filter=characters_plain&sortorder=desc")
    Suggestions getSuggestions(@Query("query") String query, @Query("page") Integer pageNumber, @Query("rp") Integer resultPerPage);

    /**
     * Query for the wiki pages of a given key. There is a wiki page per language.
     *
     * @param key The requested wiki page.
     * @return The wiki pages.
     */
    @GET("/key/wiki_pages")
    List<Wiki> getWikiPages(@Query("key") String key);

    /**
     * Query for the tags combinations of a given key.
     *
     * @param key The key.
     * @param pageNumber The page number, if null return results of the first page.
     * @param resultPerPage The number of results per page, if null return all the results.
     * @return The tags combinations.
     */
    @GET("/key/combinations?sortname=to_count&sortorder=desc&qtype=other_key&filter=all")
    Combinations getCombinations(@Query("key") String key, @Query("page") Integer pageNumber, @Query("rp") Integer resultPerPage);
}
