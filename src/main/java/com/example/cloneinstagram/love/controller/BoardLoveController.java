package com.example.cloneinstagram.love.controller;

import com.example.cloneinstagram.commonDto.ResponseMsgDto;
import com.example.cloneinstagram.love.service.BoardLoveService;
import com.example.cloneinstagram.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardLoveController {

    private final BoardLoveService boardLoveService;

    //피드 좋아요
    @PostMapping("/boards/{boardId}")
    public ResponseMsgDto boardLove(@PathVariable(name = "boardId") Long id, @AuthenticationPrincipal UserDetailsImpl userDetails){
        return boardLoveService.boardLove(id, userDetails.getUser());
    }
}
