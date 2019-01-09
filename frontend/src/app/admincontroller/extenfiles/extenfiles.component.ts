import { Component, OnInit } from '@angular/core';
import { FileService } from 'src/app/servers.service';
import { Response } from '@angular/http';
import { Router } from '@angular/router';
import { CanComponentDeactivate } from './candeactivate-guard.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-extenfiles',
  templateUrl: './extenfiles.component.html',
  styleUrls: ['./extenfiles.component.css']
})


export class ExtenfilesComponent implements OnInit, CanComponentDeactivate {
  selectedFileTypeId = 0;
  selectedExtensionId = 0;

  fileTypeArr = [];
  extensionsList = [];

  extensionsArr = [];
  changesSaved = false;
  

  constructor(private fileService: FileService, private router: Router) { }

  ngOnInit() {
    this.retriveFilesTypes();
    this.retriveExtension();
  }

  /** 
   * To reterive file types
  */
  retriveFilesTypes() {
    this.fileService.getExtnFiles()
      .subscribe(
        (response: Response) => {
          this.fileTypeArr = response.json();
          console.log("Data is fetching...", this.fileTypeArr);
        },
        (error) => console.log(error)
      ); 
  }

  /** 
   * To reterive extensions
  */

  retriveExtension(){
    this.fileService.getExtensions()
      .subscribe(
        (response: Response) => {
          this.extensionsList = response.json();
          console.log("Data is fetching...", this.extensionsList);
        },
        (error) => console.log(error)
      );
  }

  onFileTypeSelect(fileTypeId) {
    this.fileService.getExtnFiletypesById(fileTypeId)
      .subscribe(
        (response: Response) => {
          this.extensionsArr = response.json();
          console.log("FilterTypeArr Data is fetching...", this.extensionsArr);
        },
        (error) => console.log(error)
      );
  }


 

  onExtensionSelect(id) {
    console.log("selected extension id", id);
    if(this.extensionsArr && this.extensionsArr.findIndex(ext => ext.extensionId == id) > -1){
      this.selectedExtensionId = 0;
    }else if(this.selectedFileTypeId > 0){
      let selectedExtension  = this.extensionsList.filter(ext => ext.id == id)[0];
      let selectedFileType = this.fileTypeArr.filter(file => file.filetypeId == this.selectedFileTypeId)[0];
      this.extensionsArr.push({key: {extensionId : selectedExtension.id, filetypeId: selectedFileType.filetypeId}, extension : selectedExtension.name, extensionId : selectedExtension.id, file: selectedFileType.file, filetypeId : selectedFileType.filetypeId})

    }
  }


  removeExtension(extension){
    console.log("remove Extension", extension);
    let index = this.extensionsArr.indexOf(extension);
    console.log("index", index);
    this.extensionsArr.splice(index,1);
  }

  onSaveClick(){
    this.fileService.saveExtensionFile(this.extensionsArr)
      .subscribe(
        (response: Response) => {console.log("this.ext",response);
          this.changesSaved=true;
      },
        (error) => console.log(error)
      );
  }

  canDeactivate(): Observable<boolean> | Promise<boolean> | boolean {
    if (!(this.changesSaved)) {
      return confirm("Do you want to discard the changes?");
    }
    else {
      return true;
    }
  }
}
