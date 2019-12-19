export class SearchState {
    static KEY = 'search-state';
    environments = '';
    applications = '';
    hostnames = '';
    includeIfCollectedForAtLeastDays = 30;
    includeIfNotInvokedInDays = 30;
    includeUntrackedMethods = false;
    includeOnlyNeverInvokedMethods = false;

    public isShowingEverything() {
        return this.isEmpty(this.environments) && this.isEmpty(this.applications) && this.isEmpty(this.hostnames);
    }

    private isEmpty(s: string) {
        return !s || s.trim().length === 0
    }
}
