import { Component, OnInit } from '@angular/core';
import { SpinnerService } from '../../service/spinner.service';

@Component({
  selector: 'app-spinner',
  templateUrl: './spinner.component.html',
  styleUrls: ['./spinner.component.css']
})
export class SpinnerComponent implements OnInit {

  IsLoading$ = this.spinnerService.IsLoading$;

  constructor(private spinnerService : SpinnerService) { }

  ngOnInit(): void {
  }

}
