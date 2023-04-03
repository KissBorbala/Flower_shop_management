import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../services/user.service';
import { SnackbarService } from '../services/snackbar.service';
import { MatDialogRef } from '@angular/material/dialog';
import { NgxUiLoaderService } from 'ngx-ui-loader';
import { GlobalConstatns } from '../shared/global-constants';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent implements OnInit {

  password = true;
  confirmPassword = true;
  signupForm:any = FormGroup;
  responseMessage:any;

  constructor(
    private formBuilder:FormBuilder,
    private router:Router,
    private userService:UserService,
    private snackbarService:SnackbarService,
    public dialogRef:MatDialogRef<SignupComponent>,
    private ngxService:NgxUiLoaderService
  ) { }

  ngOnInit(): void {
    this.signupForm = this.formBuilder.group({
      firstName:[null, [Validators.required, Validators.pattern(GlobalConstatns.nameRegex)]],
      lastName:[null, [Validators.required, Validators.pattern(GlobalConstatns.nameRegex)]],
      email:[null, [Validators.required, Validators.pattern(GlobalConstatns.emailRegex)]],
      phoneNr:[null, [Validators.required, Validators.pattern(GlobalConstatns.phoneNrRegex)]],
      password:[null, [Validators.required]],
      confirmPassword:[null, [Validators.required]]
    })
  }

  validateSubmit() {
    if(this.signupForm.controls['password'].value != this.signupForm.controls['confirmPassword'].value) {
      return true;
    }
    else {
      return false;
    }
  }

  handleSubmit() {
    this.ngxService.start();
    var formData = this.signupForm.value;
    var data = {
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      phoneNr: formData.phoneNr,
      password: formData.password
    }

    this.userService.signup(data).subscribe((response:any)=> {
      this.ngxService.stop();
      this.dialogRef.close();
      this.responseMessage = response?.message;
      this.snackbarService.openSnackBar(this.responseMessage, "");
      this.router.navigate(['/']);

    }, (error) => {
      this.ngxService.stop();
      if(error.error?.message) {
        this.responseMessage = error.error?.message;
      }
      else {
        this.responseMessage = GlobalConstatns.genericError;
      }
      this.snackbarService.openSnackBar(this.responseMessage, GlobalConstatns.error);
    })
  }
}
