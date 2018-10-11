/**
 * The state for MethodsComponent.
 */
import {DashboardApiService} from '../../services/dashboard-api.service';
import {MethodData} from '../../model/methods/MethodData';
import {MethodsComponent} from './methods.component';
import {Method} from '../../model/methods/Method';
import {GetMethodsRequest} from '../../model/methods/GetMethodsRequest';

export class CheckboxState {
    constructor(public name: string, public selected: boolean) {
    }
}

export class MethodsComponentState {
    static KEY = 'methods';

    req = new GetMethodsRequest();
    includeIfNotInvokedInDays = 30;
    includeSyntheticMethods = false;
    includeUntrackedMethods = false;
    includeOnlyNeverInvokedMethods = false;
    data: MethodData;
    errorMessage: string;
    sortColumn = MethodsComponent.SIGNATURE_COLUMN;
    sortAscending = true;
    selectedMethod: Method;
    searching = false;

    applications: CheckboxState[] = [];
    environments: CheckboxState[] = [];

    constructor(private api: DashboardApiService) {
    }

    initialize() {

        let getState = function (name: string, oldState: CheckboxState[]) {
            let old: boolean[] = oldState.filter(cs => cs.name === name).map(cs => cs.selected);
            return old.length > 0 ? old[0] : false;
        };

        let copyNames = function (checkboxState: CheckboxState[], newNames: string[]) {
            let oldState = Object.assign([], checkboxState);
            checkboxState.length = 0;
            newNames.forEach(name => checkboxState.push(new CheckboxState(name, getState(name, oldState))));
        };

        this.api.getMethodsFormData().subscribe(data => {
            console.log('[ck dashboard] methodsFormData=%o', data);
            copyNames.call(null, this.applications, data.applications);
            copyNames.call(null, this.environments, data.environments);

            if (!this.anyEnvironmentSelected()) {
                // Try to pre-select any environment named 'prod*'
                this.environments.forEach(e => {
                    if (e.name.toLowerCase().startsWith('prod')) {
                        e.selected = true;
                    }
                });
            }
        });
    }

    anyApplicationSelected() {
        return this.applications.some(s => s.selected === true);
    }

    anyEnvironmentSelected() {
        return this.environments.some(s => s.selected === true);
    }

    headerIconClassesSignature() {
        return this.getHeaderIconClassesFor(MethodsComponent.SIGNATURE_COLUMN);
    }

    headerIconClassesAge() {
        return this.getHeaderIconClassesFor(MethodsComponent.AGE_COLUMN);
    }

    headerIconClassesCollectedDays() {
        return this.getHeaderIconClassesFor(MethodsComponent.COLLECTED_DAYS_COLUMN);
    }

    rowIconClasses(id: number) {
        let visible = this.selectedMethod && this.selectedMethod.id === id;
        return {
            'fas': visible,
            'fa-ellipsis-h': visible
        }
    }

    sortBySignature() {
        this.sortBy(MethodsComponent.SIGNATURE_COLUMN);
    }

    sortByAge() {
        this.sortBy(MethodsComponent.AGE_COLUMN);
    }

    sortByCollectedDays() {
        this.sortBy(MethodsComponent.COLLECTED_DAYS_COLUMN);
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
            } else if (this.sortColumn === MethodsComponent.COLLECTED_DAYS_COLUMN) {
                cmp = m1.collectedDays - m2.collectedDays;
            }
            if (cmp === 0) {
                // Make sure the sorting is stable
                cmp = m1.id - m2.id;
            }
            return this.sortAscending ? cmp : -cmp;
        });
    }

    getInvokedBefore(): Date {
        let d = new Date();
        d.setDate(d.getDate() - this.includeIfNotInvokedInDays);
        return d;
    }

    search() {
        this.searching = true;
        this.req.suppressSyntheticMethods = !this.includeSyntheticMethods;
        this.req.suppressUntrackedMethods = !this.includeUntrackedMethods;
        this.req.onlyInvokedBeforeMillis = this.getCutoffTimeMillis();
        this.req.applications = this.applications.filter(s => s.selected).map(s => s.name);
        this.req.environments = this.environments.filter(s => s.selected).map(s => s.name);

        this.api
            .getMethods(this.req)
            .subscribe(data => {
                this.data = data;
                this.errorMessage = undefined;
                if (this.data.methods.length === 1) {
                    this.selectMethod(this.data.methods[0]);
                } else if (this.selectedMethod) {
                    let previouslySelected = this.data.methods.find(m => m.id === this.selectedMethod.id);
                    this.selectMethod(previouslySelected);
                } else {
                    this.selectMethod(null);
                }
                this.searching = false;
            }, error => {
                this.data = undefined;
                this.errorMessage = error.statusText ? error.statusText : error;
                this.selectMethod(null);
                this.searching = false;
            });
    }

    selectMethod(m: Method) {
        this.selectedMethod = m;
    }

    isSelectedMethod(m: Method) {
        return this.selectedMethod && this.selectedMethod.id === m.id;
    }

    private sortBy(column: string) {
        if (this.sortColumn === column) {
            this.sortAscending = !this.sortAscending;
        } else {
            this.sortColumn = column;
        }
    }

    private getHeaderIconClassesFor(column: string) {
        return {
            'fas': true,
            'fa-sort-down': this.sortAscending,
            'fa-sort-up': !this.sortAscending,
            'invisible': column !== this.sortColumn // avoid column width fluctuations
        };
    }

    private getCutoffTimeMillis(): number {
        return this.includeOnlyNeverInvokedMethods ? 0 : this.getInvokedBefore().getTime();
    }

}

