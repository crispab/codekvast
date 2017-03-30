import {Component} from '@angular/core';
import {WarehouseService} from './warehouse.service';
import {MethodData} from './model/MethodData';
import {AgePipe} from './age.pipe';
import {DatePipe} from '@angular/common';
import {Method} from './model/Method';

@Component({
    selector: 'ck-methods',
    template: require('./methods.component.html'),
    styles: [require('./methods.component.css')],
    providers: [WarehouseService, AgePipe, DatePipe],
})
export class MethodsComponent {

    signature: string;
    maxResults = 100;
    data: MethodData;
    errorMessage: string;
    dateFormat = 'age';
    sortColumn = 'signature';
    sortAscending = true;

    constructor(private warehouse: WarehouseService) {
    }

    sortBy(column: string) {
        if (this.sortColumn === column) {
            this.sortAscending = !this.sortAscending;
        } else {
            this.sortColumn = column;
        }
        console.log(`Sorting by ${this.sortColumn}, ascending=${this.sortAscending}`);
    }

    search() {
        this.warehouse
            .getMethods(this.signature, this.maxResults)
            .subscribe(data => {
                this.data = data;
                this.errorMessage = undefined;
            }, error => {
                this.data = undefined;
                this.errorMessage = error;
            }, () => console.log('getMethods() complete'));
    }

    sortedMethods(): Method[] {
        if (!this.data) {
            return null;
        }
        const greaterThan = this.sortAscending ? +1 : -1;
        return this.data.methods.sort((m1, m2) => {
           if (this.sortColumn === 'signature') {
               if (m1.signature > m2.signature) {
                   return greaterThan;
               } else if (m1.signature === m2.signature) {
                   return 0;
               } else {
                   return -greaterThan;
               }
           } else if (this.sortColumn === 'age') {
               return this.sortAscending
                   ? m2.lastInvokedAtMillis - m1.lastInvokedAtMillis
                   : m1.lastInvokedAtMillis - m2.lastInvokedAtMillis;
           } else {
               return 0;
           }
        });
    }
}
