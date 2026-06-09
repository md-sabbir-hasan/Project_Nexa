package com.nexaerp.journal;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.journal.dto.JournalEntryRequestDto;
import com.nexaerp.journal.dto.JournalEntryResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService journalEntryService;


    @PostMapping
    public ResponseEntity<ApiResponse<JournalEntryResponseDto>> create(
            @Valid @RequestBody JournalEntryRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Journal entry created",
                journalEntryService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JournalEntryResponseDto>> update(@PathVariable Long id, @Valid @RequestBody JournalEntryRequestDto request){
        return ResponseEntity.ok(ApiResponse.success("Journal Entry Updated", journalEntryService.update(id, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JournalEntryResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(journalEntryService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JournalEntryResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(journalEntryService.getAll()));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<ApiResponse<JournalEntryResponseDto>> post(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Journal entry posted",
                journalEntryService.post(id)));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<ApiResponse<JournalEntryResponseDto>> reverse(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Journal entry reversed",
                journalEntryService.reverse(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        journalEntryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Journal entry deleted", null));
    }





}
