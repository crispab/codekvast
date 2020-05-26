/**
 * The state for MethodsComponent.
 */
import {MethodData} from '../../model/methods/method-data';
import {Method} from '../../model/methods/method';
import {GetMethodsRequest} from '../../model/methods/get-methods-request';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {StateService} from '../../services/state.service';
import {SearchState} from '../../model/search-state';
import {ClientSettings} from '../../model/client-settings';

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
  data: MethodData;
  errorMessage: string;
  sortColumn = MethodsComponentState.SIGNATURE_COLUMN;
  sortAscending = true;
  selectedMethod: Method;
  searching = false;

  searchState: SearchState;
  applications: string[] = [];
  environments: string[] = [];
  locations: string[] = [];
  retentionPeriodDays = -1;
  firstTime = true;
  settings: ClientSettings;

  constructor(private api: DashboardApiService, private stateService: StateService) {
    this.searchState = this.stateService.getState(SearchState.KEY, () => new SearchState());
    this.settings = this.stateService.getState(ClientSettings.KEY, () => new ClientSettings())
  }

  initialize() {
    this.api.getMethodsFormData().subscribe(data => {
      console.log('[ck dashboard] methodsFormData=%o', data);
      this.applications = data.applications;
      this.environments = data.environments;
      this.locations = data.locations;
      this.retentionPeriodDays = data.retentionPeriodDays;

      if (this.firstTime) {
        if (this.retentionPeriodDays > 0) {
          this.searchState.includeIfCollectedForAtLeastDays = this.retentionPeriodDays;
          this.searchState.includeIfNotInvokedInDays = Math.min(this.retentionPeriodDays, 7);
        } else {
          this.searchState.includeIfCollectedForAtLeastDays = 30;
          this.searchState.includeIfNotInvokedInDays = 30;
        }
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

  methodComparator(m1: Method, m2: Method): number {
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
  }

  sortedMethods(): Method[] {
    if (!this.data || !this.data.methods) {
      return null;
    }

    return this.data.methods.filter((m: Method) => !Method.hasAnnotation(m)).sort(this.methodComparator.bind(this));
  }

  sortedAnnotatedMethods(): Method[] {
    if (!this.data || !this.data.methods) {
      return null;
    }
    return this.data.methods.filter((m: Method) => Method.hasAnnotation(m)).sort(this.methodComparator.bind(this));
  }

  annotatedMethodExist(): boolean {
    if (!this.data || !this.data.methods) {
      return false;
    }
    return this.data.methods.find((m: Method) => Method.hasAnnotation(m)) !== undefined
  }

  getInvokedBefore(): Date {
    let d = new Date();
    d.setDate(d.getDate() - this.searchState.includeIfNotInvokedInDays);
    return d;
  }

  getFilteredApplications() {
    return this.applications.filter(a => a.toLowerCase().indexOf(this.searchState.applications.trim().toLowerCase()) >= 0);
  }

  getFilteredEnvironments() {
    return this.environments.filter(a => a.toLowerCase().indexOf(this.searchState.environments.trim().toLowerCase()) >= 0);
  }

  getFilteredLocations() {
    return this.locations.filter(a => a.toLowerCase().indexOf(this.searchState.locations.trim().toLowerCase()) >= 0);
  }

  isSearchDisabled(): Boolean {
    return this.searching
        || this.getFilteredApplications().length == 0
        || this.getFilteredEnvironments().length == 0
        || (this.locations.length > 0 && this.getFilteredLocations().length == 0);
  }

  search() {
    this.searching = true;
    this.req.suppressUntrackedMethods = !(this.settings.advancedControls && this.searchState.includeUntrackedMethods);
    this.searchState.maxResults = Math.min(this.searchState.maxResults, 10000);
    this.req.maxResults = this.searchState.maxResults;
    this.req.minCollectedDays = this.searchState.includeIfCollectedForAtLeastDays;
    this.req.onlyInvokedBeforeMillis = this.getCutoffTimeMillis();
    this.req.applications = this.getFilteredApplications();
    this.req.environments = this.getFilteredEnvironments();
    this.req.locations = this.searchState.locations.trim().length === 0 ? null : this.getFilteredLocations();

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
    return (this.settings.advancedControls && this.searchState.includeOnlyNeverInvokedMethods) ? 0 : this.getInvokedBefore().getTime();
  }

}

