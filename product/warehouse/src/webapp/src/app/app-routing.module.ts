import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Status} from './status.component';
import {Methods} from './methods.component';

const routes: Routes = [
    {
        path: '',
        redirectTo: 'methods',
        pathMatch: 'full'
    }, {
        path: 'methods',
        component: Methods
    }, {
        path: 'status',
        component: Status
    }, {
        path: '**',
        redirectTo: 'methods',
        pathMatch: 'full'
    }
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
