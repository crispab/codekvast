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
    static readonly SIGNATURE_COLUMN = 'signature';
    static readonly AGE_COLUMN = 'age';

    signature: string;
    maxResults = 100;
    data: MethodData;
    errorMessage: string;
    dateFormat = 'age';
    sortColumn = MethodsComponent.SIGNATURE_COLUMN;
    sortAscending = true;
    selectedMethod: Method;

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

    private getHeaderIconClassesFor(column: string) {
        return {
            'fa': true,
            'fa-sort-asc': this.sortAscending,
            'fa-sort-desc': !this.sortAscending,
            'invisible': column !== this.sortColumn // avoid column width fluctuations
        };
    }

    headerIconClassesSignature() {
        return this.getHeaderIconClassesFor(MethodsComponent.SIGNATURE_COLUMN);
    }

    headerIconClassesAge() {
        return this.getHeaderIconClassesFor(MethodsComponent.AGE_COLUMN);
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
            if (cmp === 0) {
                // Make sure the sorting is stable
                cmp = m1.id - m2.id;
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
                if (this.data.methods.length === 1) {
                    this.selectMethod(this.data.methods[0]);
                } else {
                    this.selectMethod(null);
                }
            }, error => {
                this.data = undefined;
                this.errorMessage = error;
            }, () => console.log('getMethods() complete'));
    }

    selectMethod(m: Method) {
        this.selectedMethod = m;
    }

    prettyPrintSignature(m: Method) {
        console.log('Selected: %o', m);

        let result = '';
        if (m.modifiers) {
            result += m.modifiers + ' ';
        }
        result += m.signature;
        return result;
    }

    rowIconClasses(m: Method) {
        let visible = this.selectedMethod && this.selectedMethod.id === m.id;
        return {
            'fa': true,
            'fa-arrow-right': true,
            'invisible': !visible,
        };
    }
}
