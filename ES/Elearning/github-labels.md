# GitHub Label Categories

This proposes a methodology for naming and applying labels to issues and pull requests of a GitHub repository in ES project.

This document serves primarily as a guideline.  
Repositories are free to extend, but NOT diverge from here.

## Label naming and categorization

Most labels should have the form `area:name`,  
where `area` is a label category, and `name` is the actual name in kebab-lower-case.

Since labels can be searched by partial or complete words, this nomenclature does not compromise their searchability.

A list of categories with suggested initial follow.

## Important categories

Repositories are strongly recommended to have labels in these categories.  

### category:*

The kind of issue/pull request reported.  
All issues should have one of these categories, and rarely more than one.

Preferred color: shades of grey.

The following labels should exist:

- `category:bug`  
- `category:security`  
- `category:performance`  
- `category:discussion`  
- `category:documentation`  
- `category:enhancement`  
- `category:ux`  
- `category:new`  
- `category:tracker`  
- `category:chore`  

### area:*

Used to specify what main component of the software is involved.  
There can be no fixed list of suggested areas, as they depend on the repository.  

Preferred color: shades of yellow.

Examples:  
- `area:frontend`  
- `area:backend`  
- `area:microservice1`  

### priority:*

No priority label means an issue is to be triaged.  
Issues should have a priority before being moved to a Sprint.  
Never more than one priority.  

Labels:  
- `priority:low`  
- `priority:normal`  
- `priority:high`  
- `priority:critical`  

### exclusion:*

For issues or pull requests which were disregarded.

- `exclusion:duplicate`  
- `exclusion:invalid`  
- `exclusion:wontfix`  

## Useful categories

These categories are optional, to be created on demand.

### status:*

Mark pull request state.

- `status:in-stage`  
- `status:blocked`  

### effort:*

For education & difficulty. Useful in assignment and prioritization.

- `effort:easy`  
- `effort:medium`  
- `effort:hard`  
- `effort:very-hard`  

### feature:*

For grouping issues/PRs under specific features.

Examples:  
- `feature:export-excel`  
- `feature:survey`  

## Additional notes

- Do not be afraid to have many labels, as long as they are distinct.  
- Use milestones for releases and contractual deliveries.  
