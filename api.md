# Reward Service API

<details>
  <summary><strong>Table of Contents</strong></summary>

  * [Query](#query)
  * [Mutation](#mutation)
  * [Objects](#objects)
    * [PaginationInfo](#paginationinfo)
    * [RewardLogItem](#rewardlogitem)
    * [RewardScore](#rewardscore)
    * [RewardScores](#rewardscores)
    * [ScoreboardItem](#scoreboarditem)
  * [Inputs](#inputs)
    * [DateTimeFilter](#datetimefilter)
    * [IntFilter](#intfilter)
    * [Pagination](#pagination)
    * [StringFilter](#stringfilter)
  * [Enums](#enums)
    * [RewardChangeReason](#rewardchangereason)
    * [SortDirection](#sortdirection)
  * [Scalars](#scalars)
    * [Boolean](#boolean)
    * [Date](#date)
    * [DateTime](#datetime)
    * [Float](#float)
    * [Int](#int)
    * [LocalTime](#localtime)
    * [String](#string)
    * [Time](#time)
    * [UUID](#uuid)
    * [Url](#url)

</details>

## Query
<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>userCourseRewardScores</strong></td>
<td valign="top"><a href="#rewardscores">RewardScores</a>!</td>
<td>


Get the reward score of the current user for the specified course.
🔒 The user must have access to the course with the given id to access their scores, otherwise an error is thrown.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">courseId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>courseRewardScoresForUser</strong></td>
<td valign="top"><a href="#rewardscores">RewardScores</a>!</td>
<td>


Get the reward score of the specified user for the specified course.
🔒 The user be an admin in the course with the given courseId to perform this action.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">courseId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>scoreboard</strong></td>
<td valign="top">[<a href="#scoreboarditem">ScoreboardItem</a>!]!</td>
<td>


Gets the power scores for each user in the course, ordered by power score descending.
🔒 The user must have access to the course with the given id to access the scoreboard, otherwise an error is thrown.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">courseId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

## Mutation
<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>recalculateScores</strong> ⚠️</td>
<td valign="top"><a href="#rewardscores">RewardScores</a>!</td>
<td>


    ONLY FOR TESTING PURPOSES. DO NOT USE IN FRONTEND. WILL BE REMOVED.

    Triggers the recalculation of the reward score of the user.
    This is done automatically at some time in the night.

    The purpose of this mutation is to allow testing of the reward score and demonstrate the functionality.
    🔒 The user be an admin in the course with the given courseId to perform this action.

<p>⚠️ <strong>DEPRECATED</strong></p>
<blockquote>

Only for testing purposes. Will be removed.

</blockquote>
</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">courseId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

## Objects

### PaginationInfo


Return type for information about paginated results.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>page</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The current page number.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>size</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The number of elements per page.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>totalElements</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The total number of elements across all pages.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>totalPages</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The total number of pages.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>hasNext</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


Whether there is a next page.

</td>
</tr>
</tbody>
</table>

### RewardLogItem


An item in the reward score log.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>date</strong></td>
<td valign="top"><a href="#datetime">DateTime</a>!</td>
<td>


The date when the reward score changed.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>difference</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The difference between the previous and the new reward score.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>oldValue</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The old reward score.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>newValue</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The new reward score.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>reason</strong></td>
<td valign="top"><a href="#rewardchangereason">RewardChangeReason</a>!</td>
<td>


The reason why the reward score has changed.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>associatedContentIds</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


The ids of the contents that are associated with the change.

</td>
</tr>
</tbody>
</table>

### RewardScore


The reward score of a user.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>value</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The absolute value of the reward score.
Health and fitness are between 0 and 100.
Growth, strength and power can be any non-negative integer.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>percentage</strong></td>
<td valign="top"><a href="#float">Float</a>!</td>
<td>


The relative value of the reward score.
Shows how many points relative to the total points have been achieved.
Only used for growth currently.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>log</strong></td>
<td valign="top">[<a href="#rewardlogitem">RewardLogItem</a>!]!</td>
<td>


A log of the changes to the reward score, ordered by date descending.

</td>
</tr>
</tbody>
</table>

### RewardScores


The five reward scores of a user.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>health</strong></td>
<td valign="top"><a href="#rewardscore">RewardScore</a>!</td>
<td>


Health represents how up-to-date the user is with the course.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>fitness</strong></td>
<td valign="top"><a href="#rewardscore">RewardScore</a>!</td>
<td>


Fitness represents how well the user repeats previously learned content.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>growth</strong></td>
<td valign="top"><a href="#rewardscore">RewardScore</a>!</td>
<td>


Growth represents the overall progress of the user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>strength</strong></td>
<td valign="top"><a href="#rewardscore">RewardScore</a>!</td>
<td>


Strength is earned by competing with other users.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>power</strong></td>
<td valign="top"><a href="#rewardscore">RewardScore</a>!</td>
<td>


A composite score of all the other scores.

</td>
</tr>
</tbody>
</table>

### ScoreboardItem


An item in the scoreboard.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>userId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


The user id of the user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>powerScore</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The power score of the user.

</td>
</tr>
</tbody>
</table>

## Inputs

### DateTimeFilter


Filter for date values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>after</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


If specified, filters for dates after the specified value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>before</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


If specified, filters for dates before the specified value.

</td>
</tr>
</tbody>
</table>

### IntFilter


Filter for integer values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>equals</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


An integer value to match exactly.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>greaterThan</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


If specified, filters for values greater than to the specified value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>lessThan</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


If specified, filters for values less than to the specified value.

</td>
</tr>
</tbody>
</table>

### Pagination


Specifies the page size and page number for paginated results.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>page</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The page number, starting at 0.
If not specified, the default value is 0.
For values greater than 0, the page size must be specified.
If this value is larger than the number of pages, an empty page is returned.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>size</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The number of elements per page.

</td>
</tr>
</tbody>
</table>

### StringFilter


Filter for string values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>equals</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>


A string value to match exactly.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contains</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>


A string value that must be contained in the field that is being filtered.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>ignoreCase</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


If true, the filter is case-insensitive.

</td>
</tr>
</tbody>
</table>

## Enums

### RewardChangeReason


The reason why the reward score has changed.

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>CONTENT_DONE</strong></td>
<td>


The user has completed a content for the first time.
The associated contents are the content that were completed.

</td>
</tr>
<tr>
<td valign="top"><strong>CONTENT_REVIEWED</strong></td>
<td>


The user has reviewed a content.
The associated contents are the content that were reviewed.

</td>
</tr>
<tr>
<td valign="top"><strong>CONTENT_DUE_FOR_LEARNING</strong></td>
<td>


There exists a content that is due for learning.
The associated contents are the content that are due for learning.

</td>
</tr>
<tr>
<td valign="top"><strong>CONTENT_DUE_FOR_REPETITION</strong></td>
<td>


There exists a content that is due for repetition.
The associated contents are the content that are due for repetition.

</td>
</tr>
<tr>
<td valign="top"><strong>COMPOSITE_VALUE</strong></td>
<td>


The score changed because the underlying scores changed.
Relevant for the power score.

</td>
</tr>
</tbody>
</table>

### SortDirection


Specifies the sort direction, either ascending or descending.

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>ASC</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>DESC</strong></td>
<td></td>
</tr>
</tbody>
</table>

## Scalars

### Boolean

Built-in Boolean

### Date

An RFC-3339 compliant Full Date Scalar

### DateTime

A slightly refined version of RFC-3339 compliant DateTime Scalar

### Float

Built-in Float

### Int

Built-in Int

### LocalTime

24-hour clock time value string in the format `hh:mm:ss` or `hh:mm:ss.sss`.

### String

Built-in String

### Time

An RFC-3339 compliant Full Time Scalar

### UUID

A universally unique identifier compliant UUID Scalar

### Url

A Url scalar

