export class SearchState {
  static KEY = 'search-state';
  environments = '';
  applications = '';
  versions = '';
  locations = '';
  hostnames = '';
  maxResults = 100;
  includeIfCollectedForAtLeastDays = 30;
  includeIfNotInvokedInDays = 30;
  includeUntrackedMethods = false;
  includeOnlyNeverInvokedMethods = false;

  public isShowingEverything() {
    return this.isEmpty(this.applications)
        && this.isEmpty(this.versions)
        && this.isEmpty(this.environments)
        && this.isEmpty(this.locations)
        && this.isEmpty(this.hostnames);
  }

  private isEmpty(s: string) {
    return !s || s.trim().length === 0
  }
}
