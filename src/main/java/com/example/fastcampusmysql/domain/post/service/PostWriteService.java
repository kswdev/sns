package com.example.fastcampusmysql.domain.post.service;

import com.example.fastcampusmysql.domain.post.dto.PostCommand;
import com.example.fastcampusmysql.domain.post.entity.Post;
import com.example.fastcampusmysql.domain.post.mapper.PostMapper;
import com.example.fastcampusmysql.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PostWriteService {
    final private PostMapper postMapper;
    final private PostRepository postRepository;
    public Long create(PostCommand command)
    {
        Post post = postMapper.toPostEntity(command);
        return postMapper.toPostDto(postRepository.save(post)).id();
    }
}