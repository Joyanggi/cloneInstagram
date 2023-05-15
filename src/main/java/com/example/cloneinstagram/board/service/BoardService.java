package com.example.cloneinstagram.board.service;

import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.cloneinstagram.board.dto.BoardRequestDto;
import com.example.cloneinstagram.board.dto.BoardResponseDto;
import com.example.cloneinstagram.board.entity.Board;
import com.example.cloneinstagram.board.repository.BoardRepository;
import com.example.cloneinstagram.common.ResponseMsgDto;
import com.example.cloneinstagram.exception.CustomException;
import com.example.cloneinstagram.exception.ErrorCode;
import com.example.cloneinstagram.member.entity.Member;
import com.example.cloneinstagram.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final AmazonS3Client amazonS3Client;
    private final BoardRepository boardRepository;
    private static final String S3_BUCKET_PREFIX = "S3";

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    Board board;

    // 게시글 작성
    @Transactional
    public ResponseMsgDto<?> createBoard(MultipartFile image, BoardRequestDto boardRequestDto, UserDetailsImpl userDetails) throws IOException {
        // 파일명 새로 부여를 위한 현재 시간 알아내기
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();
        int millis = now.get(ChronoField.MILLI_OF_SECOND);

        String imageUrl = null;

        // 새로 부여한 이미지명
        String newFileName = "image" + hour + minute + second + millis;
        String fileExtension = '.' + image.getOriginalFilename().replaceAll("^.*\\.(.*)$", "$1");
        String imageName = S3_BUCKET_PREFIX + newFileName + fileExtension;

        // 메타데이터 설정
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(image.getContentType());
        objectMetadata.setContentLength(image.getSize());

        InputStream inputStream = image.getInputStream();

        amazonS3Client.putObject(new PutObjectRequest(bucketName, imageName, inputStream, objectMetadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));
        imageUrl = amazonS3Client.getUrl(bucketName, imageName).toString();

        board = Board.builder()
                .imageUrl(imageUrl)
                .contents(boardRequestDto.getContents())
                .member(userDetails.getUser())
                .build();

        boardRepository.save(board);

        return ResponseMsgDto.setSuccess(HttpStatus.OK.value(), "게시글 작성 완료", new BoardResponseDto(board));
    }

    // 게시글 수정
    @Transactional
    public ResponseMsgDto<?> updatePost(Long id, BoardRequestDto boardRequestDto, Member member) {
        matchAuthor(id, member);
        board = existBoard(id);
        board.update(boardRequestDto);
        return ResponseMsgDto.setSuccess(HttpStatus.OK.value(), "게시글 수정 완료",  new BoardResponseDto(board));
    }

    // 게시글 삭제
    @Transactional
    public ResponseMsgDto<?> deletePost(Long id, Member member) {
        matchAuthor(id, member);
        board = existBoard(id);
        boardRepository.deleteById(id);
        return ResponseMsgDto.setSuccess(HttpStatus.OK.value(), "게시글 삭제 완료", null);
    }

    // 게시글 DB 존재 여부 확인
    private Board existBoard(Long id) {
        board = boardRepository.findById(id).orElseThrow(
                () -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        return board;
    }

    // 작성자 일치 여부 확인
    private void matchAuthor(Long id, Member member) {
        board = boardRepository.findByIdAndNickName(id, member.getNickName()).orElseThrow(
                () -> new CustomException(ErrorCode.AUTHOR_NOT_SAME_MOD)
        );
    }
}