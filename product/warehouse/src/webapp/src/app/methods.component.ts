import {Component, ViewChild} from '@angular/core';
import {WarehouseService} from './warehouse.service';
import {MethodData} from './model/MethodData';
import {AgePipe} from './age.pipe';
import {DatePipe} from '@angular/common';
import {Method} from './model/Method';
import {MethodDetailComponent} from './method-detail.component';

@Component({
    selector: 'ck-methods',
    template: require('./methods.component.html'),
    styles: [require('./methods.component.css')],
    providers: [WarehouseService, AgePipe, DatePipe],
})
export class MethodsComponent {
    static readonly SIGNATURE_COLUMN = 'signature';
    static readonly AGE_COLUMN = 'age';

    signature: string;
    maxResults = 100;
    data: MethodData;
    errorMessage: string;
    dateFormat = 'age';
    sortColumn = MethodsComponent.SIGNATURE_COLUMN;
    sortAscending = true;

    @ViewChild('methodDetail') public methodDetail: MethodDetailComponent;

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

    private getHeaderClassesFor(column: string) {
        return {
            'fa': true,
            'fa-sort-asc': this.sortAscending,
            'fa-sort-desc': !this.sortAscending,
            'invisible': column !== this.sortColumn // avoid column width fluctuations
        };
    }

    headerClassesSignature() {
        return this.getHeaderClassesFor(MethodsComponent.SIGNATURE_COLUMN);
    }

    headerClassesAge() {
        return this.getHeaderClassesFor(MethodsComponent.AGE_COLUMN);
    }

    sortBySignature() {
        this.sortBy(MethodsComponent.SIGNATURE_COLUMN);
    }

    sortByAge() {
        this.sortBy(MethodsComponent.AGE_COLUMN);
    }

    sortedMethods(): Method[] {
        if (!this.data || !this.data.methods) {
            return null;
        }

        return this.data.methods.sort((m1: Method, m2: Method) => {
            let cmp = 0;
            if (this.sortColumn === MethodsComponent.SIGNATURE_COLUMN) {
                cmp = m1.signature.localeCompare(m2.signature);
            } else if (this.sortColumn === MethodsComponent.AGE_COLUMN) {
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

    showMethodDetail(m: Method) {
        this.methodDetail.method = m;
    }

    rowClasses(m: Method) {
        let visible = this.methodDetail && this.methodDetail.method && this.methodDetail.method.id === m.id;
        return {
            'fa': true,
            'fa-arrow-right': true,
            'invisible': !visible,
        };
    }
}
