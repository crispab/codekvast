/**
 * The state for MethodsComponent.
 */
import {MethodData} from '../../model/methods/method-data';
import {Method} from '../../model/methods/method';
import {GetMethodsRequest} from '../../model/methods/get-methods-request';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {StateService} from '../../services/state.service';
import {SearchState} from '../../model/search-state';

export class CheckboxState {
    constructor(public name: string, public selected: boolean) {
    }
}

export class MethodsComponentState {
    static KEY = 'methods';
    static readonly SIGNATURE_COLUMN = 'signature';
    static readonly AGE_COLUMN = 'age';
    static readonly COLLECTED_DAYS_COLUMN = 'collectedDays';


    req = new GetMethodsRequest();
    includeIfNotInvokedInDays = 30;
    includeUntrackedMethods = false;
    includeOnlyNeverInvokedMethods = false;
    data: MethodData;
    errorMessage: string;
    sortColumn = MethodsComponentState.SIGNATURE_COLUMN;
    sortAscending = true;
    selectedMethod: Method;
    searching = false;

    searchState: SearchState;
    applications: string[] = [];
    environments: string[] = [];
    retentionPeriodDays = -1;
    firstTime = true;

    constructor(private api: DashboardApiService, private stateService: StateService) {
        this.searchState = this.stateService.getState(SearchState.KEY, () => new SearchState());
    }

    initialize() {
        this.api.getMethodsFormData().subscribe(data => {
            console.log('[ck dashboard] methodsFormData=%o', data);
            this.applications = data.applications;
            this.environments = data.environments;
            this.retentionPeriodDays = data.retentionPeriodDays;

            if (this.firstTime) {
                if (!this.searchState.environments && this.environments.map(a => a.toLowerCase()).filter(a => a.indexOf('prod') >= 0)) {
                    this.searchState.environments = 'prod';
                }
                this.req.minCollectedDays = this.retentionPeriodDays > 0 ? this.retentionPeriodDays : 30;
                this.firstTime = false;
            }
        });
    }

    headerIconClassesSignature() {
        return this.getHeaderIconClassesFor(MethodsComponentState.SIGNATURE_COLUMN);
    }

    headerIconClassesAge() {
        return this.getHeaderIconClassesFor(MethodsComponentState.AGE_COLUMN);
    }

    headerIconClassesCollectedDays() {
        return this.getHeaderIconClassesFor(MethodsComponentState.COLLECTED_DAYS_COLUMN);
    }

    rowIconClasses(id: number) {
        let visible = this.selectedMethod && this.selectedMethod.id === id;
        return {
            fas: visible,
            'fa-ellipsis-h': visible
        };
    }

    sortBySignature() {
        this.sortBy(MethodsComponentState.SIGNATURE_COLUMN);
    }

    sortByAge() {
        this.sortBy(MethodsComponentState.AGE_COLUMN);
    }

    sortByCollectedDays() {
        this.sortBy(MethodsComponentState.COLLECTED_DAYS_COLUMN);
    }

    sortedMethods(): Method[] {
        if (!this.data || !this.data.methods) {
            return null;
        }

        return this.data.methods.sort((m1: Method, m2: Method) => {
            let cmp = 0;
            if (this.sortColumn === MethodsComponentState.SIGNATURE_COLUMN) {
                cmp = m1.signature.localeCompare(m2.signature);
            } else if (this.sortColumn === MethodsComponentState.AGE_COLUMN) {
                cmp = m1.lastInvokedAtMillis - m2.lastInvokedAtMillis;
            } else if (this.sortColumn === MethodsComponentState.COLLECTED_DAYS_COLUMN) {
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

    getFilteredApplications() {
        return this.applications.filter(a => a.toLowerCase().indexOf(this.searchState.applications.toLowerCase()) >= 0);
    }

    getFilteredEnvironments() {
        return this.environments.filter(a => a.toLowerCase().indexOf(this.searchState.environments.toLowerCase()) >= 0);
    }

    search() {
        this.searching = true;
        this.req.suppressUntrackedMethods = !this.includeUntrackedMethods;
        this.req.onlyInvokedBeforeMillis = this.getCutoffTimeMillis();
        this.req.applications = this.getFilteredApplications();
        this.req.environments = this.getFilteredEnvironments();

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

    minCollectedDaysMax() {
        return this.retentionPeriodDays > 0 ? this.retentionPeriodDays : 3650;
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
            fas: true,
            'fa-sort-down': this.sortAscending,
            'fa-sort-up': !this.sortAscending,
            invisible: column !== this.sortColumn // avoid column width fluctuations
        };
    }

    private getCutoffTimeMillis(): number {
        return this.includeOnlyNeverInvokedMethods ? 0 : this.getInvokedBefore().getTime();
    }

}

