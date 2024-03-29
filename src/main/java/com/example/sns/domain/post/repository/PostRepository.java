package com.example.sns.domain.post.repository;

import com.example.sns.util.PageHelper;
import com.example.sns.domain.post.dto.DailyPostCount;
import com.example.sns.domain.post.dto.DailyPostCountRequest;
import com.example.sns.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class PostRepository {
    final static String TABLE = "Post";
    final private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    final static private RowMapper<Post> ROW_MAPPER =
            (ResultSet resultSet, int rowNum) -> Post.builder()
                    .id(resultSet.getLong("id"))
                    .memberId(resultSet.getLong("memberId"))
                    .contents(resultSet.getString("contents"))
                    .createdDate(resultSet.getObject("createdDate", LocalDate.class))
                    .createdAt(resultSet.getObject("createdAt", LocalDateTime.class))
                    .likeCount(resultSet.getLong("likeCount"))
                    .version(resultSet.getLong("version"))
                    .build();

    final static private RowMapper<DailyPostCount> DAILY_POST_COUNT_MAPPER =
            (ResultSet resultSet, int rowNum) -> new DailyPostCount(
                resultSet.getLong("memberId"),
                resultSet.getObject("createdDate", LocalDate.class),
                resultSet.getLong("count")
            );

    public List<DailyPostCount> groupByCreatedDate(DailyPostCountRequest request) {
        String sql = String.format("""
                SELECT
                    createdDate, memberId, count(id) as count
                FROM %s
                WHERE memberId = :memberId and createdDate BETWEEN :firstDate and :lastDate
                GROUP BY createdDate, memberId
                """, TABLE);
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(request);
        return namedParameterJdbcTemplate.query(sql, params, DAILY_POST_COUNT_MAPPER);
    }

    private Long getCount(Long memberId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberId", memberId);

        String sql = String.format("""
                SELECT COUNT(id)
                FROM %s
                WHERE memberId = :memberId
                """, TABLE);
        return namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
    }

    public List<Post> findByAllByInId(List<Long> ids) {
        if(ids.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", ids);

        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE id in (:ids)
                """, TABLE);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public Optional<Post> findByById(Long id, boolean requiredLock) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);

        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE id = :id
                """, TABLE);
        if(requiredLock) {
            sql += "FOR UPDATE";
        }
        Post nullablePost = namedParameterJdbcTemplate.queryForObject(sql, params, ROW_MAPPER);
        return Optional.ofNullable(nullablePost);
    }

    public Page<Post> findAllByMemberId(Long memberId, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("size", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());

        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE memberId = :memberId
                ORDER BY %s
                LIMIT :size
                OFFSET :offset
                """, TABLE, PageHelper.orderBy(pageable.getSort()));
        List<Post> posts = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
        return new PageImpl<>(posts, pageable, getCount(memberId));
    }

    public List<Post> findAllByMemberIdAndOrderByIdDesc(Long memberId, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("size", size);

        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE memberId = :memberId
                ORDER BY id DESC
                LIMIT :size
                """, TABLE);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }
    public List<Post> findAllByInMemberIdAndOrderByIdDesc(List<Long> memberIds, int size) {
        if(memberIds.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberIds", memberIds)
                .addValue("size", size);

        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE memberId in (:memberIds)
                ORDER BY id DESC
                LIMIT :size
                """, TABLE);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Post> findAllByInMemberIdLessThanIdAndOrderByIdDesc(Long id, List<Long> memberIds, int size) {
        if(memberIds.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberIds", memberIds)
                .addValue("size", size)
                .addValue("id", id);

        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE memberId in (:memberIds) and id < :id
                ORDER BY id DESC
                LIMIT :size
                """, TABLE);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }
    public List<Post> findAllByMemberIdLessThanIdAndOrderByIdDesc(Long id, Long memberId, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("size", size)
                .addValue("id", id);

        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE memberId = :memberId and id < :id
                ORDER BY id DESC
                LIMIT :size
                """, TABLE);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }
    public Post save(Post post) {
        if(post.getId() == null) {
            return insert(post);
        }
        return update(post);
    }

    public void bulkInsert(List<Post> posts) {
        var sql = String.format("""
                INSERT INTO %s (memberId, contents, createdDate, createdAt)
                VALUES (:memberId, :contents, :createdDate, :createdAt)
                """, TABLE);

        SqlParameterSource[] params = posts
                .stream()
                .map(BeanPropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new);
        namedParameterJdbcTemplate.batchUpdate(sql, params);
    }
    private Post insert(Post post) {
        SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName(TABLE)
                .usingGeneratedKeyColumns("id");

        SqlParameterSource params = new BeanPropertySqlParameterSource(post);
        Long id = jdbcInsert.executeAndReturnKey(params).longValue();

        return Post.builder()
                .id(id)
                .memberId(post.getMemberId())
                .contents(post.getContents())
                .createdDate(post.getCreatedDate())
                .createdAt(post.getCreatedAt())
                .build();
    }

    private Post update(Post post) {
        String sql = String.format("""
                UPDATE %s SET
                    memberId = :memberId
                    ,contents = :contents
                    ,createdDate = :createdDate
                    ,likeCount = :likeCount
                    ,createdAt = :createdAt
                    ,version = :version + 1
                WHERE id = :id and version = :version
                """, TABLE);
        SqlParameterSource params = new BeanPropertySqlParameterSource(post);
        int updatedCount = namedParameterJdbcTemplate.update(sql, params);

        if(updatedCount == 0) {
            throw new RuntimeException("갱신 실패");
        }
        return post;
    }
}
