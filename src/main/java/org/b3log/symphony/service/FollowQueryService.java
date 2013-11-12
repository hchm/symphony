/*
 * Copyright (c) 2009, 2010, 2011, 2012, 2013, B3log Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.symphony.service;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.CompositeFilter;
import org.b3log.latke.repository.CompositeFilterOperator;
import org.b3log.latke.repository.Filter;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.SortDirection;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.util.CollectionUtils;
import org.b3log.symphony.model.Follow;
import org.b3log.symphony.repository.FollowRepository;
import org.b3log.symphony.repository.UserRepository;
import org.b3log.symphony.util.Filler;
import org.json.JSONObject;

/**
 * Follow query service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Nov 11, 2013
 * @since 0.2.5
 */
@Service
public class FollowQueryService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(FollowQueryService.class.getName());

    /**
     * Follow repository.
     */
    @Inject
    private FollowRepository followRepository;

    /**
     * User repository.
     */
    @Inject
    private UserRepository userRepository;
    
    /**
     * Filler.
     */
    @Inject
    private Filler filler;

    /**
     * Determines whether exists a follow relationship for the specified follower and the specified following entity.
     *
     * @param followerId the specified follower id
     * @param followingId the specified following entity id
     * @return {@code true} if exists, returns {@code false} otherwise
     */
    public boolean isFollowing(final String followerId, final String followingId) {
        try {
            return followRepository.exists(followerId, followingId);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Determines following failed[followerId=" + followerId + ", followingId="
                                    + followingId + ']', e);

            return false;
        }
    }

    /**
     * Gets following users of the specified follower.
     *
     * @param followerId the specified follower id
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return following users, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getFollowingUsers(final String followerId, final int currentPageNum, final int pageSize)
            throws ServiceException {
        final List<JSONObject> ret = new ArrayList<JSONObject>();

        try {
            final List<JSONObject> followings = getFollowings(followerId, pageSize, currentPageNum, Follow.FOLLOWING_TYPE_C_USER);

            for (final JSONObject follow : followings) {
                final String followingId = follow.optString(Follow.FOLLOWING_ID);
                final JSONObject user = userRepository.get(followingId);

                if (null == user) {
                    LOGGER.log(Level.WARN, "Not found user[id=" + followingId + ']');

                    continue;
                }
                
                filler.fillUserThumbnailURL(user);

                ret.add(user);
            }
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets following users of follower[id=" + followerId + "] failed", e);

        }
        
        return ret;
    }

    /**
     * Gets follower users of the specified following user.
     *
     * @param followingUserId the specified following user id
     * @param currentPageNum the specified page number
     * @param pageSize the specified page size
     * @return follower users, returns an empty list if not found
     * @throws ServiceException service exception
     */
    public List<JSONObject> getFollowerUsers(final String followingUserId, final int currentPageNum, final int pageSize)
            throws ServiceException {
        final List<JSONObject> ret = new ArrayList<JSONObject>();

        try {
            final List<JSONObject> followers = getFollowers(followingUserId, pageSize, currentPageNum, Follow.FOLLOWING_TYPE_C_USER);

            for (final JSONObject follow : followers) {
                final String followerId = follow.optString(Follow.FOLLOWER_ID);
                final JSONObject user = userRepository.get(followerId);

                if (null == user) {
                    LOGGER.log(Level.WARN, "Not found user[id=" + followerId + ']');

                    continue;
                }
                
                filler.fillUserThumbnailURL(user);

                ret.add(user);
            }
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets follower users of following user[id=" + followingUserId + "] failed", e);
        }
        
        return ret;
    }

    /**
     * Gets the followings of a follower specified by the given follower id and following type.
     *
     * @param followerId the given follower id
     * @param followingType the specified following type
     * @param currentPageNum the specified current page number
     * @param pageSize the specified page size
     * @return followings, returns an empty list if not found
     * @throws RepositoryException repository exception
     */
    private List<JSONObject> getFollowings(final String followerId, final int followingType, final int currentPageNum, final int pageSize)
            throws RepositoryException {
        final List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PropertyFilter(Follow.FOLLOWER_ID, FilterOperator.EQUAL, followerId));
        filters.add(new PropertyFilter(Follow.FOLLOWING_TYPE, FilterOperator.EQUAL, followingType));

        final Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).
                setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters))
                .setPageSize(pageSize).setCurrentPageNum(currentPageNum);

        final JSONObject result = followRepository.get(query);

        return CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
    }

    /**
     * Gets the followers of a following specified by the given following id and follow type.
     *
     * @param followingId the given following id
     * @param followingType the specified following type
     * @param currentPageNum the specified current page number
     * @param pageSize the specified page size
     * @return followers, returns an empty list if not found
     * @throws RepositoryException repository exception
     */
    private List<JSONObject> getFollowers(final String followingId, final int followingType, final int currentPageNum, final int pageSize)
            throws RepositoryException {
        final List<Filter> filters = new ArrayList<Filter>();
        filters.add(new PropertyFilter(Follow.FOLLOWING_ID, FilterOperator.EQUAL, followingId));
        filters.add(new PropertyFilter(Follow.FOLLOWING_TYPE, FilterOperator.EQUAL, followingType));

        final Query query = new Query().addSort(Keys.OBJECT_ID, SortDirection.DESCENDING).
                setFilter(new CompositeFilter(CompositeFilterOperator.AND, filters))
                .setPageSize(pageSize).setCurrentPageNum(currentPageNum);

        final JSONObject result = followRepository.get(query);

        return CollectionUtils.<JSONObject>jsonArrayToList(result.optJSONArray(Keys.RESULTS));
    }
}