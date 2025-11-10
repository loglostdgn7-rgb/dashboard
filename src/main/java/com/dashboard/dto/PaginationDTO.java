package com.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaginationDTO {
    private List<EventDTO> eventList;

    private int totalCount;
    private int totalPages;
    private int currentPage;
    private int startPage;
    private int endPage;
    private boolean hasPrev;
    private boolean hasNext;


    public PaginationDTO(int totalCount, int currentPage, int pageSize, int pageBlockSize, List<EventDTO> eventList) {
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.eventList = eventList;

        this.totalPages = (int) Math.ceil((double) totalCount / pageSize);

        //보여줄 페이지 번호 계산 (5개 단위)
        int currentBlock = (int) Math.ceil((double) currentPage / pageBlockSize);
        this.startPage = (currentBlock - 1) * pageBlockSize + 1;
        this.endPage = this.startPage + pageBlockSize - 1;

        if (this.endPage > this.totalPages) {
            this.endPage = this.totalPages;
        }

        this.hasPrev = this.startPage > 1;
        this.hasNext= this.endPage < this.totalPages;
    }

}
