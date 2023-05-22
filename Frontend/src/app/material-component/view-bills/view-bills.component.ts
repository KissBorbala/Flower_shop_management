import { Component, OnInit } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { NgxUiLoaderService } from 'ngx-ui-loader';
import { BillService } from 'src/app/services/bill.service';
import { SnackbarService } from 'src/app/services/snackbar.service';
import { GlobalConstatns } from 'src/app/shared/global-constants';
import { ViewBillProductsComponent } from '../dialog/view-bill-products/view-bill-products.component';
import { ConfirmationComponent } from '../dialog/confirmation/confirmation.component';
import * as saveAs from 'file-saver';

@Component({
  selector: 'app-view-bills',
  templateUrl: './view-bills.component.html',
  styleUrls: ['./view-bills.component.scss']
})
export class ViewBillsComponent implements OnInit {

  displayedColumns: string[] = ['name', 'email', 'phoneNr', 'paymentMethod', 'total', 'view'];
  dataSource:any;
  responseMessage:any;


  constructor(
    private billService:BillService,
    private ngxService:NgxUiLoaderService,
    private dialog:MatDialog,
    private snackbarService:SnackbarService,
    private router:Router) { }

    ngOnInit(): void {
      this.ngxService.start();
      this.tableData();
    }
  
    tableData() {
      this.billService.getBills().subscribe((response:any)=>{
          this.ngxService.stop();
          this.dataSource = new MatTableDataSource(response);
      }, (error:any)=>{
        this.ngxService.stop();
        console.log(error.error?.message);
        if(error.error?.message) {
          this.responseMessage = error.error?.message;
        }
        else {
          this.responseMessage = GlobalConstatns.genericError;
        }
        this.snackbarService.openSnackBar(this.responseMessage, GlobalConstatns.error);
      })
    }

    applyFilter(event:Event){
      const filterValue = (event.target as HTMLInputElement).value;
      this.dataSource.filter = filterValue.trim().toLowerCase();
    }

    handleViewAction(values:any) {
      const dialogConfig = new MatDialogConfig();
      dialogConfig.data ={
        data: values
      };
      dialogConfig.width = "100%";
      const dialogRef = this.dialog.open(ViewBillProductsComponent, dialogConfig);
      this.router.events.subscribe(()=> {
        dialogRef.close();
      })
    }

    downloadReportAction(values:any) {
      this.ngxService.start();
      var data = {
        name: values.name,
        email:values.email,
        uuid:values.uuid,
        phoneNr:values.phoneNr,
        paymentMethod:values.patmentMethod,
        totalAmount:values.values.total.toString(),
        productDetails:values.productDetails
      }
      this.downloadFile(values.uuid, data);
    }

    handleDeleteAction(values:any) {
      const dialogConfig = new MatDialogConfig();
      dialogConfig.data ={
        message:'delete ' + values.name + ' product',
        confirmation:true
      };
      const dialogRef = this.dialog.open(ConfirmationComponent, dialogConfig);
      const sub = dialogRef.componentInstance.onEmitStatusChange.subscribe((response)=> {
        this.ngxService.start();
        this.deleteBill(values.id);
        dialogRef.close();
      })
    }

    deleteBill(id:any) {
      this.billService.delete(id).subscribe((response:any)=>{
        this.ngxService.stop();
        this.tableData();
        this.responseMessage = response.message;
        this.snackbarService.openSnackBar(this.responseMessage, "Success");
      }, (error:any) => {
        this.ngxService.stop();
        console.log(error);
        if(error.error?.error) {
          this.responseMessage = error.error?.error;
        }
        else {
          this.responseMessage = GlobalConstatns.genericError;
        }
        this.snackbarService.openSnackBar(this.responseMessage, GlobalConstatns.error);
      })
    }

    downloadFile(fileName: string, data:any) {
      this.billService.getPdf(data).subscribe((response)=> {
        saveAs(response, fileName + '.pdf');
        this.ngxService.stop();
      })
    }

}
