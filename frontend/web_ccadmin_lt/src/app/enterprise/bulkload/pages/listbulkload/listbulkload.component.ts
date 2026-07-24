import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { BulkLoadConstants } from '../../model/BulkLoadConstants';
import { BulkLoadHead, PageResponse } from '../../model/BulkLoadModels';
import { BulkLoadService } from '../../service/bulk-load.service';

@Component({
  selector: 'app-listbulkload',
  templateUrl: './listbulkload.component.html',
  styleUrls: ['./listbulkload.component.css']
})
export class ListBulkLoadComponent implements OnInit {
  readonly constants = BulkLoadConstants;
  response: PageResponse<BulkLoadHead> = this.emptyPage();
  loading = false;
  filter = {
    Query: '',
    BulkLoadType: '',
    ProcessStatus: '',
    DateStart: null as string | null,
    DateEnd: null as string | null,
    Page: 1
  };

  constructor(
    private service: BulkLoadService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    void this.findAll(1);
  }

  async findAll(page: number): Promise<void> {
    this.loading = true;
    this.filter.Page = page;
    try {
      const response = await this.service.findAll(this.filter);
      if (response.ErrorStatus) {
        this.toastr.error(response.Message);
        return;
      }
      this.response = response.Data as PageResponse<BulkLoadHead>;
    } finally {
      this.loading = false;
    }
  }

  progress(value: number | null | undefined): number {
    return Math.min(100, Math.max(0, Number(value ?? 0)));
  }

  pages(): number[] {
    const total = this.response.TotalPages ?? 0;
    const current = this.response.Page ?? 1;
    const start = Math.max(1, current - 2);
    const end = Math.min(total, current + 2);
    const pages: number[] = [];
    for (let page = start; page <= end; page++) pages.push(page);
    return pages;
  }

  private emptyPage(): PageResponse<BulkLoadHead> {
    return {
      resultSearch: [],
      TotalPages: 0,
      TotalResult: 0,
      StarResult: 0,
      EndResult: 0,
      Page: 1
    };
  }
}
