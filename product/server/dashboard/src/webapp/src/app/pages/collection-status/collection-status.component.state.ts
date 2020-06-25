import {AgePipe} from '../../pipes/age.pipe';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {StatusData} from '../../model/status/status-data';
import {Subscription, timer} from 'rxjs';
import {StateService} from '../../services/state.service';
import {SearchState} from '../../model/search-state';

export class CollectionStatusComponentState {
  static KEY = 'collection-status';

  data: StatusData;
  errorMessage: string;
  autoRefresh = true;
  showTerminatedAgents = false;
  refreshIntervalSeconds = 60;

  searchState: SearchState;

  private timerSubscription: Subscription;

  constructor(private agePipe: AgePipe, private api: DashboardApiService, private stateService: StateService) {
  }

  init() {
    this.searchState = this.stateService.getState(SearchState.KEY, () => new SearchState());
    if (this.autoRefresh) {
      this.startAutoRefresh();
    } else {
      this.refreshNow();
    }
  }

  destroy() {
    this.stopAutoRefresh();
  }

  autoRefreshButtonClasses() {
    return {
      fas: true,
      'fa-pause': this.autoRefresh,
      'fa-play': !this.autoRefresh
    };
  }

  toggleAutoRefresh() {
    this.autoRefresh = !this.autoRefresh;
    if (!this.autoRefresh) {
      this.stopAutoRefresh();
    } else {
      this.startAutoRefresh();
    }
  }

  autoRefreshButtonText() {
    return this.autoRefresh ? 'Pause auto-refresh' : 'Resume auto-refresh';
  }

  updateRefreshTimer() {
    this.refreshIntervalSeconds = Math.max(10, this.refreshIntervalSeconds);
    console.log('[ck dashboard] New refreshIntervalSeconds: %o', this.refreshIntervalSeconds);
    if (this.autoRefresh) {
      this.stopAutoRefresh();
      this.startAutoRefresh();
    }
  }

  refreshNow() {
    this.api
    .getStatus()
    .subscribe(data => {
      this.data = data;
      this.errorMessage = undefined;
    }, error => {
      this.data = undefined;
      this.errorMessage = error.statusText ? error.statusText : error;
    });
  }

  communicationFailure() {
    let now = this.agePipe.transform(new Date(), 'shortTime');
    return now + ': Communication failure';
  }

  getFilteredEnvironments() {
    if (this.data.environments) {
      let envRegExp = new RegExp(this.searchState.environments, 'i');
      return this.data.environments.filter(e => envRegExp.test(e.name));
    }
    return null;
  }

  getFilteredApplications() {
    if (this.data.applications) {
      let appRegExp = new RegExp(this.searchState.applications, 'i');
      let envRegExp = new RegExp(this.searchState.environments, 'i');
      return this.data.applications.filter(a => appRegExp.test(a.appName) && envRegExp.test(a.environment));
    }
    return null;
  }

  getFilteredAgents() {
    if (this.data.agents) {
      let appRegExp = new RegExp(this.searchState.applications, 'i');
      let versionRegExp = new RegExp(this.searchState.versions, 'i');
      let envRegExp = new RegExp(this.searchState.environments, 'i');
      let hostRegExp = new RegExp(this.searchState.hostnames, 'i');
      return this.data.agents.filter(a => appRegExp.test(a.appName)
          && versionRegExp.test(a.appVersion)
          && envRegExp.test(a.environment)
          && hostRegExp.test(a.hostname));
    }
    return null;
  }

  getVisibleAgents() {
    return this.getFilteredAgents().filter(a => (this.showTerminatedAgents || a.agentAlive));
  }

  numTerminatedAgents() {
    if (this.data.agents) {
      return this.data.agents.filter(a => !a.agentAlive).length;
    }
    return null;
  }

  numTerminatedFilteredAgents() {
    if (this.getFilteredAgents()) {
      return this.getFilteredAgents().filter(a => !a.agentAlive).length;
    }
    return null;
  }

  private startAutoRefresh() {
    this.timerSubscription = timer(0, this.refreshIntervalSeconds * 1000).subscribe((tick: number) => {
      console.log('[ck dashboard] Doing auto-refresh #%o', tick);
      this.refreshNow();
    });
  }

  private stopAutoRefresh() {
    this.timerSubscription.unsubscribe();
  }

}
