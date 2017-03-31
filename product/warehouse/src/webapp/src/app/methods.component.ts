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
    private SIGNATURE = 'signature';
    private AGE = 'age';

    signature: string;
    maxResults = 100;
    data: MethodData;
    errorMessage: string;
    dateFormat = 'age';
    sortColumn = this.SIGNATURE;
    sortAscending = true;

    constructor(private warehouse: WarehouseService) {
    }

    private sortBy(column: string) {
        if (this.sortColumn === column) {
            this.sortAscending = !this.sortAscending;
        } else {
            this.sortColumn = column;
        }
        console.log(`Sorting by ${this.sortColumn}, ascending=${this.sortAscending}`);
    }

    private getHeaderClasses(c: string) {
        return {
            'fa': true,
            'fa-sort-asc': this.sortAscending,
            'fa-sort-desc': !this.sortAscending,
            'invisible': c !== this.sortColumn // avoid column width fluctuations
        };
    }

    headerClassesSignature() {
        return this.getHeaderClasses(this.SIGNATURE);
    }

    headerClassesAge() {
        return this.getHeaderClasses(this.AGE);
    }

    sortBySignature() {
        this.sortBy(this.SIGNATURE);
    }

    sortByAge() {
        this.sortBy(this.AGE);
    }

    sortedMethods(): Method[] {
        if (!this.data || !this.data.methods) {
            return null;
        }

        return this.data.methods.sort((m1: Method, m2: Method) => {
            let cmp = 0;
            if (this.sortColumn === this.SIGNATURE) {
                cmp = m1.signature.localeCompare(m2.signature);
            } else if (this.sortColumn === this.AGE) {
                cmp = m1.lastInvokedAtMillis - m2.lastInvokedAtMillis;
            }
            return this.sortAscending ? cmp : -cmp;
        });
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

}
